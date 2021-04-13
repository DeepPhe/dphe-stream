package org.healthnlp.deepphe.nlp.ae;

import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.core.util.Pair;
import org.apache.ctakes.core.util.regex.RegexSpanFinder;
import org.apache.ctakes.typesystem.type.textspan.Sentence;
import org.apache.log4j.Logger;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.healthnlp.deepphe.nlp.uri.UriAnnotationFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author SPF , chip-nlp
 * @since {4/2/2021}
 */
@PipeBitInfo(
      name = "BiomarkerFinder",
      description = "Finds Biomarker values.",
      products = { PipeBitInfo.TypeProduct.IDENTIFIED_ANNOTATION, PipeBitInfo.TypeProduct.GENERIC_RELATION },
      role = PipeBitInfo.Role.ANNOTATOR )
final public class BiomarkerFinder extends JCasAnnotator_ImplBase {

   static private final Logger LOGGER = Logger.getLogger( "BiomarkerFinder" );





   static private final String REGEX_METHOD
         = "IHC|Immunohistochemistry|ISH|(?:IN SITU HYBRIDIZATION)|(?:DUAL ISH)"
           + "|FISH|(?:Fluorecent IN SITU HYBRIDIZATION)|(?:Nuclear Staining)";
   // for

   static private final String REGEX_TEST = "Test|Method";

   static private final String REGEX_LEVEL = "Level|status|expression|result|results|score";

//   static private final String REGEX_IS = "is|are|was";

   static private final String REGEX_STRONGLY = "weakly|strongly|greatly";
   static private final String REGEX_RISING = "rising|increasing|elevated|elvtd|raised|increased|strong|amplified";
   static private final String REGEX_FALLING = "falling|decreasing|low|lowered|decreased|weak";
   static private final String REGEX_STABLE = "stable";


   static private final String REGEX_GT_LT = "(?:(?:Greater|\\>|Higher|Less|\\<|Lower)(?: than ?)?)?"
                                             + "(?: or )?(?:Greater|\\>|Higher|Less|\\<|Lower|Equal|\\=)(?: than|to "
                                             + "?)?";

   static private final String REGEX_POSITIVE = "\\+?pos(?:itive|itivity)?|\\+(?:pos)?|overexpression";
   static private final String REGEX_NEGATIVE = "\\-?neg(?:ative)?|\\-(?:neg)?|(?:not amplified)|(?:no [a-z] detected)";
   static private final String REGEX_UNKNOWN
         = "unknown|indeterminate|equivocal|borderline|(?:not assessed|requested|applicable)|\\bN\\/?A\\b";
   static private final String REGEX_POS_NEG = "(?:" + REGEX_POSITIVE + ")|(?:" + REGEX_NEGATIVE + ")";
   static private final String REGEX_POS_NEG_UNK
         = "(?:" + REGEX_POSITIVE + ")|(?:" + REGEX_NEGATIVE + ")|(?:" + REGEX_UNKNOWN + ")";

   static private final String REGEX_0_9
         = "[0-9]|zero|one|two|three|four|five|six|seven|eight|nine";

   static private final String REGEX_NUMTEEN
         = "(?:1[0-9])|ten|eleven|twelve|thirteen|fourteen|fifteen|sixteen|seventeen|eighteen|nineteen";
   static private final String REGEX_0_19 = REGEX_0_9 + "|" + REGEX_NUMTEEN;

   static private final String REGEX_NUMTY
         = "(?:[2-9]0)|twenty|thirty|forty|fifty|sixty|seventy|eighty|ninety";
   static private final String REGEX_0_99
         = "(?:" + REGEX_0_19 + ")|(?:(?:" + REGEX_NUMTY + ")(?: ?-? ?" + REGEX_0_9 + ")?)";

   static private final String REGEX_HUNDREDS
         = "(?:[1-9]00)|(?:(?:" + REGEX_0_9 + " ?-? )?hundred)";
   static private final String REGEX_0_999
         = "(?:" + REGEX_0_99 + ")|(?:" + REGEX_HUNDREDS + ")(: ?-? ?" + REGEX_0_99 + ")?)";

   static private final String REGEX_DECIMAL = "\\.[0-9]{1,4}";




   private enum Biomarker {
//      ER( ),
//      PR,
//      HER2,
      KI67( "M?KI ?-? ?67(?: Antigen)?",
            "",
            "(?:>|< ?)?[0-9]{1,2}(?:\\.[0-9]{1,2} ?)? ?%(?: positive)?",
            true ),

      BRCA1( "(?:BRCA1|BROVCA1|(?:Breast Cancer Type 1))"
             + "(?: Susceptibility)?(?: Gene)?(?: Polymorphism)?",
             "",
             "" ),

      BRCA2( "(?:BRCA2|BROVCA2|FANCD1|(?:Breast Cancer Type 2))"
             + "(?: Susceptibility)?(?: Gene)?(?: Polymorphism)?",
             "",
             "" ),

      ALK( "(?:ALK|CD246|(?:Anaplastic Lymphoma (?:Receptor Tyrosine )?Kinase))"
           + "(?: Fusion)?(?: Gene|Oncogene)?(?: Alteration)?",
           "",
           REGEX_POS_NEG_UNK,
           true ),

      EGFR( "EGFR|HER1||ERBB|C-ERBB1|(?:Epidermal Growth Factor)"
            + "(?: Receptor)?",
            "",
            REGEX_POS_NEG_UNK,
            true ),

      BRAF( "(?:Serine\\/Threonine-Protein Kinase )?B-?RAF1?"
            + "(?: Fusion)?",
            "",
            "" ),

      ROS1( "(?:Proto-Oncogene )?(?:ROS1|MCF3|C-ROS-1"
            + "|(?:ROS Proto-Oncogene 1)"
            + "|(?:Tyrosine-Protein Kinase ROS)"
            + "|(?:Receptor Tyrosine Kinase c-ROS Oncogene 1))"
            + "(?: Gene)?(?: Fusion|Alteration|Rearrangement)?",
            "",
            REGEX_POS_NEG_UNK,
            true ),

//      PD1,
      PDL1( "(?:PDL1|PD-L1|CD247|B7|B7-H|B7H1|PDCD1L1|PDCD1LG1|(?:Programmed Cell Death 1 Ligand 1))"
            + "(?: Antigen)?(?: Molecule)?",
            "",
            "" ),

      MSI( "MSI|MSS|Microsatellite",
           "",
           "stable" ),

      KRAS( "(?:KRAS|C-K-RAS|KRAS2|KRAS-2|V-KI-RAS2|(?:Kirsten Rat Sarcoma Viral Oncogene Homolog))"
            + "(?: Wildtype|wt)?(?: Gene Mutation)?",
            "",
            REGEX_POS_NEG_UNK,
            true ),

      PSA( "PSA(?: Prostate Specific Antigen)?|Prostate Specific Antigen(?: [PSA])?",
           "[0-9]{3}\\.[0-9]{2}",
           "[0-9]{1,2}(?:\\.[0-9]{1,4})?(?: ?ng\\/m?d?L)?" ),

     PSA_EL( "PSA(?: Prostate Specific Antigen)?|Prostate Specific Antigen(?: [PSA])?",
          "",
          REGEX_RISING,
             true );

      final Pattern _typePattern;
      final int _windowSize;
      final boolean _checkSkip;
      final Pattern _skipPattern;
      final Pattern _valuePattern;
      final boolean _canPrecede;
      Biomarker( final String typeRegex, final String skipRegex, final String valueRegex ) {
         this( typeRegex, 20, skipRegex, valueRegex, false );
      }
      Biomarker( final String typeRegex, final String skipRegex, final String valueRegex,
                 final boolean canPrecede ) {
         this( typeRegex, 20, skipRegex, valueRegex, canPrecede );
      }
      Biomarker( final String typeRegex, final int windowSize, final String skipRegex,
                 final String valueRegex, final boolean canPrecede ) {
         _typePattern = Pattern.compile( typeRegex, Pattern.CASE_INSENSITIVE );
         _windowSize = windowSize;
         if ( skipRegex.isEmpty() ) {
            _checkSkip = false;
            _skipPattern = null;
         } else {
            _checkSkip = true;
            _skipPattern = Pattern.compile( skipRegex, Pattern.CASE_INSENSITIVE );
         }
         _valuePattern = Pattern.compile( valueRegex, Pattern.CASE_INSENSITIVE );
         _canPrecede = canPrecede;
      }
   }


   /**
    * {@inheritDoc}
    */
   @Override
   public void process( final JCas jCas ) throws AnalysisEngineProcessException {
      LOGGER.info( "Finding Biomarkers and Values ..." );

      findBiomarkers( jCas );

   }


   static public void findBiomarkers( final JCas jCas ) {
      final String docText = jCas.getDocumentText();
      final Collection<Pair<Integer>> sentenceSpans
            = JCasUtil.select( jCas, Sentence.class )
                      .stream()
                      .map( s -> new Pair<>( s.getBegin(), s.getEnd() ) )
//                      .sorted( Comparator.comparingInt( Pair::getValue1 ) )
                      .collect( Collectors.toList() );
      for ( Biomarker biomarker : Biomarker.values() ) {
         final List<Pair<Integer>> biomarkerSpans = findBiomarkerSpans( biomarker, docText );
         addBiomarkers( jCas, biomarker, docText, biomarkerSpans, sentenceSpans );
      }
   }


   static private List<Pair<Integer>> findBiomarkerSpans( final Biomarker biomarker, final String text ) {
      try ( RegexSpanFinder finder = new RegexSpanFinder( biomarker._typePattern ) ) {
         return finder.findSpans( text );
      } catch ( IllegalArgumentException iaE ) {
         LOGGER.warn( iaE.getMessage() );
         return Collections.emptyList();
      }
   }


   static private void addBiomarkers( final JCas jCas,
                                      final Biomarker biomarker,
                                       final String text,
                                       final List<Pair<Integer>> biomarkerSpans,
                                      final Collection<Pair<Integer>> sentenceSpans ) {
      if ( biomarkerSpans.isEmpty() ) {
         return;
      }
      for ( Pair<Integer> biomarkerSpan : biomarkerSpans ) {
         addBiomarker( jCas, biomarker, text, biomarkerSpan, sentenceSpans );
      }
   }


   static private void addBiomarker( final JCas jCas,
                                      final Biomarker biomarker,
                                      final String text,
                                      final Pair<Integer> biomarkerSpan,
                                     final Collection<Pair<Integer>> sentenceSpans ) {
      final Pair<Integer> sentenceSpan = getSentenceSpan( biomarkerSpan, sentenceSpans );
      if ( addBioMarkerFollowed( jCas, biomarker, text, biomarkerSpan, sentenceSpan ) ) {
         return;
      }
      addBioMarkerPreceded( jCas, biomarker, text, biomarkerSpan, sentenceSpan );
   }

   static private boolean addBioMarkerFollowed( final JCas jCas,
                                                final Biomarker biomarker,
                                                final String text,
                                                final Pair<Integer> biomarkerSpan,
                                                final Pair<Integer> sentenceSpan ) {
      final String nextText = getFollowingText( biomarker, biomarkerSpan, text, sentenceSpan );
      if ( nextText.isEmpty() ) {
         return false;
      }
      LOGGER.info( "Testing " + biomarker.name() + " > " + nextText );
      if ( biomarker._checkSkip ) {
         final Matcher skipMatcher = biomarker._skipPattern.matcher( nextText );
         if ( skipMatcher.find() ) {
            LOGGER.info( "Skipping Biomarker > because found " + nextText.substring(
                  biomarkerSpan.getValue2() + skipMatcher.start(),
                  biomarkerSpan.getValue2() + skipMatcher.end() ) );
            return false;
         }
      }
      final Matcher matcher = biomarker._valuePattern.matcher( nextText );
      if ( matcher.find() ) {
         addBiomarker( jCas, biomarker, biomarkerSpan,
                       biomarkerSpan.getValue2() + matcher.start(),
                       biomarkerSpan.getValue2() + matcher.end() );
         return true;
      }
      return false;
   }

   static private boolean addBioMarkerPreceded( final JCas jCas,
                                                final Biomarker biomarker,
                                                final String text,
                                                final Pair<Integer> biomarkerSpan,
                                                final Pair<Integer> sentenceSpan ) {
      if ( !biomarker._canPrecede ) {
         return false;
      }
      final String prevText = getPrecedingText( biomarker, biomarkerSpan, text, sentenceSpan );
      if ( prevText.isEmpty() ) {
         return false;
      }
      LOGGER.info( "Testing " + biomarker.name() + " < " + prevText );
      final Matcher matcher = biomarker._valuePattern.matcher( prevText );
      Pair<Integer> lastMatch = null;
      while ( matcher.find() ) {
         lastMatch = new Pair<>( matcher.start(), matcher.end() );
      }
      if ( lastMatch == null ) {
         return false;
      }
      addBiomarker( jCas, biomarker, biomarkerSpan,
                    biomarkerSpan.getValue1() - prevText.length() + lastMatch.getValue1(),
                    biomarkerSpan.getValue1() - prevText.length() + lastMatch.getValue2() );
      return true;
   }

   static private void addBiomarker( final JCas jCas,
                                      final Biomarker biomarker,
                                      final Pair<Integer> biomarkerSpan,
                                        final int valueSpanBegin, final int valueSpanEnd ) {
      // Biomarker type is stored in annotation preferred text
//     final Collection<IdentifiedAnnotation> markers
//            = UriAnnotationFactory.createIdentifiedAnnotations( jCas,
//                                                                biomarkerSpan.getValue1(),
//                                                                biomarkerSpan.getValue2(),
//                                                                "Biomarker", // UriConstants.BIOMARKER,
//                                                                biomarker.name() );
//      final Collection<IdentifiedAnnotation> values
//            = UriAnnotationFactory.createIdentifiedAnnotations( jCas,
//                                                                valueSpanBegin,
//                                                                valueSpanEnd,
//                                                                "LAB_VALUE", //UriConstants.LAB_VALUE,
//                                                                SemanticTui.T034.getGroup(),
//                                                                SemanticTui.T034.name() );
//      for ( IdentifiedAnnotation marker : markers ) {
//         for ( IdentifiedAnnotation value : values ) {
//            RelationUtil.createRelation( jCas, marker, value, "hasValue" ); //RelationConstants.has_Value );
//            LOGGER.info( "Created Biomarker " + IdentifiedAnnotationUtil.getPreferredText( marker ) + " " + value.getCoveredText() );
//         }
//      }

      // Create annotations with the uri of the biomarker type, over the span of the biomarker VALUE within the text.
      UriAnnotationFactory.createIdentifiedAnnotations( jCas,
                                                       valueSpanBegin,
                                                       valueSpanEnd,
                                                       biomarker.name() );
   }


   static private Pair<Integer> getSentenceSpan( final Pair<Integer> biomarkerSpan,
                                                 final Collection<Pair<Integer>> sentenceSpans ) {
      return sentenceSpans.stream()
                         .filter( s -> s.getValue1() <= biomarkerSpan.getValue1()
                                       && biomarkerSpan.getValue2() <= s.getValue2() )
                         .findFirst()
                         .orElse( biomarkerSpan );
   }


   static private String getPrecedingText( final Biomarker biomarker,
                                           final Pair<Integer> biomarkerSpan,
                                           final String text,
                                           final Pair<Integer> sentenceSpan ) {
      if ( !biomarker._canPrecede ) {
         return "";
      }
      final int leastStart = Math.min( sentenceSpan.getValue1(),
                                       Math.max( 0, biomarkerSpan.getValue1() - biomarker._windowSize ) );
      final String prevText = text.substring( leastStart, biomarkerSpan.getValue1() );
      // Check for end of paragraph
      final int pIndex = prevText.lastIndexOf( "\n\n" );
      if ( pIndex >= 0 ) {
         return prevText.substring( pIndex+2 );
      }
      return prevText;
   }


   static private String getFollowingText( final Biomarker biomarker,
                                            final Pair<Integer> biomarkerSpan,
                                            final String text,
                                           final Pair<Integer> sentenceSpan ) {
      final int mostEnd = Math.max( sentenceSpan.getValue2(),
                                    Math.min( text.length(), biomarkerSpan.getValue2() + biomarker._windowSize ) );
      final String nextText = text.substring( biomarkerSpan.getValue2(), mostEnd );
      // Check for end of paragraph
      final int pIndex = nextText.indexOf( "\n\n" );
      if ( pIndex == 0 ) {
         return "";
      }
      if ( pIndex > 0 ) {
         return nextText.substring( 0, pIndex );
      }
      return nextText;
   }



}
