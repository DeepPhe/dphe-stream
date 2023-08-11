package org.healthnlp.deepphe.nlp.ae;

import org.apache.ctakes.core.pipeline.PipeBitInfo;
import org.apache.ctakes.core.util.Pair;
import org.apache.ctakes.core.util.annotation.IdentifiedAnnotationUtil;
import org.apache.ctakes.core.util.doc.DocIdUtil;
import org.apache.ctakes.core.util.log.DotLogger;
import org.apache.ctakes.core.util.relation.RelationBuilder;
import org.apache.ctakes.typesystem.type.constants.CONST;
import org.apache.ctakes.typesystem.type.relation.BinaryTextRelation;
import org.apache.ctakes.typesystem.type.syntax.BaseToken;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.typesystem.type.textspan.ListEntry;
import org.apache.ctakes.typesystem.type.textspan.Paragraph;
import org.apache.ctakes.typesystem.type.textspan.Sentence;
import org.apache.log4j.Logger;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.healthnlp.deepphe.core.document.SectionType;
import org.healthnlp.deepphe.core.neo4j.Neo4jOntologyConceptUtil;
import org.healthnlp.deepphe.nlp.uri.UriInfoCache;
import org.healthnlp.deepphe.summary.engine.NeoplasmSummaryCreator;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static org.healthnlp.deepphe.core.document.SectionType.*;
import static org.healthnlp.deepphe.neo4j.constant.RelationConstants.*;

/**
 * @author SPF , chip-nlp
 * @since {3/7/2023}
 */
@PipeBitInfo(
      name = "CrRelationFinder",
      description = "Creates text relations.",
      role = PipeBitInfo.Role.ANNOTATOR
)
final public class CrRelationFinder extends JCasAnnotator_ImplBase {

   static private final Logger LOGGER = Logger.getLogger( "CrRelationFinder" );

   static private final double MIN_PLACEMENT_SCORE = 1;
   static private final double SENTENCE_OFFSET = 0;
   static private final double LIST_ENTRY_OFFSET = 1;
   static private final double PARAGRAPH_OFFSET = 5;
   static private final double SECTION_OFFSET = 10;
   // 4/24/2023  DOCUMENT_OFFSET from 15 to 20 since the sentence splitter can be overly enthusiastic.
   static private final double DOCUMENT_OFFSET = 20;
   static private final double SENTENCE_MULTIPLIER = 0.1;
   static private final double LIST_ENTRY_MULTIPLIER = 0.1;
   static private final double PARAGRAPH_MULTIPLIER = 0.2;
   static private final double SECTION_MULTIPLIER = 0.3;
   static private final double DOCUMENT_MULTIPLIER = .5;
   static private final double PRECEDE_MULTIPLIER = 0.1;
   static private final double FOLLOW_MULTIPLIER = 0.2;

   static private final double NEGATED_PENALTY = 30;
   static private final double UNCERTAIN_PENALTY = 5;

   static private final double FAMILY_HISTORY_PENALTY = 50;
   static private final double PERSONAL_HISTORY_PENALTY = 5;

   // 4/24/2023 MASS_PENALTY from 40 to 20, NODE_PENALTY from 50 to 25 since the sentence splitter may not split.
   static private final double MASS_BUMP = -40;
   static private final double AFFIRM_BUMP = -20;
   static private final double MASS_PENALTY = 20;
   static private final double NODE_PENALTY = 25;

   // 4/27/2023 TEST_PENALTY from 40 to 20
   static private final double TEST_PENALTY = 40;

   // 4/27/2023 NEARBY_PENALTY from 10 to 30
   static private final double NEARBY_PENALTY = 30;

   static private final double PREFERRED_SECTION_BUMP = -30;
   static private final double LESSER_SECTION_PENALTY = 20;

   static private final double SMALL_LENGTH_PENALTY = 20;
//   static private final double SMALL_LENGTH_PENALTY = 0;


   //   static private final double MIN_RELATION_CONFIDENCE = 1;
   static private final double MIN_RELATION_CONFIDENCE = 1;
   static private final double MIN_LOCATION_CONFIDENCE = 0;

   /**
    * {@inheritDoc}
    */
   @Override
   public void initialize( final UimaContext context ) throws ResourceInitializationException {
      super.initialize( context );
   }


   /**
    * {@inheritDoc}
    */
   @Override
   public void process( final JCas jCas ) throws AnalysisEngineProcessException {
      LOGGER.info( "Creating Relations ..." );
      NeoplasmSummaryCreator.addDebug( "CrRelationFinder: " + DocIdUtil.getDocumentID( jCas ) +"\n");
      try ( DotLogger dotter = new DotLogger() ) {
         final Map<String,Collection<IdentifiedAnnotation>> uriAnnotationsMap = new HashMap<>();
         final Collection<Pair<Integer>> paragraphBounds = new HashSet<>();
         final Collection<Pair<Integer>> listEntryBounds = new HashSet<>();
         final Map<String,Sentence> sentences = new HashMap<>();
         final Map<Integer,Integer> tokenBeginToTokenNums = new HashMap<>();
         final Map<Integer,Integer> tokenEndToTokenNums = new HashMap<>();
         fillSectionsMaps( jCas, uriAnnotationsMap, paragraphBounds, listEntryBounds, sentences,
                           tokenBeginToTokenNums, tokenEndToTokenNums );
         if ( uriAnnotationsMap.size() < 2 ) {
            return;
         }
         final int docLength = jCas.getDocumentText().length();
         // Loop through URIs and Annotations.
         for ( Map.Entry<String,Collection<IdentifiedAnnotation>> uriSourceAnnotations : uriAnnotationsMap.entrySet() ) {
            final String sourceUri = uriSourceAnnotations.getKey();
            final UriInfoCache.UriNode sourceNode = UriInfoCache.getInstance().getUriNode( sourceUri );
            if ( sourceNode.lacksRelations() ) {
               continue;
            }
            for ( Map.Entry<String, Collection<IdentifiedAnnotation>> uriTargetAnnotations :
                  uriAnnotationsMap.entrySet() ) {
               final String targetUri = uriTargetAnnotations.getKey();
               if ( sourceUri.equals( targetUri ) ) {
                  continue;
               }
               final Map<String,Double> relationScoresMap = sourceNode.getRelationScores( targetUri );
               if ( relationScoresMap.isEmpty() ) {
                  // Move on to next target URI.
                  continue;
               }
               for ( IdentifiedAnnotation sourceAnnotation : uriSourceAnnotations.getValue() ) {
//                  final AnnotationPenalties sourcePenalties = new AnnotationPenalties( sourceAnnotation,
                  final AnnotationPenalties sourcePenalties = new NeoplasmAnnotationPenalties( sourceAnnotation,
                                                                                                sentences,
                                                                                                tokenBeginToTokenNums,
                                                                                                tokenEndToTokenNums,
                                                                                               docLength );
                  final Map<String,Collection<RelationScore>> relatedAnnotationScoresMap = new HashMap<>();
                  for ( Map.Entry<String,Double> relationScores : relationScoresMap.entrySet() ) {
                     final Collection<RelationScore> annotationRelationScores =
                           createAnnotationRelationScores( relationScores.getKey(),
                                                           relationScores.getValue(),
                                                           sourceAnnotation,
                                                           sourcePenalties,
                                                           uriTargetAnnotations.getValue(),
                                                           sentences,
                                                           paragraphBounds,
                                                           listEntryBounds,
                                                           tokenBeginToTokenNums,
                                                           tokenEndToTokenNums,
                                                           docLength );
                     relatedAnnotationScoresMap.put( relationScores.getKey(), annotationRelationScores );
                  }
                  for ( Map.Entry<String,Collection<RelationScore>> relatedAnnotationScores
                        : relatedAnnotationScoresMap.entrySet() ) {
                     createAllRelations( jCas, sourceAnnotation,
                                          relatedAnnotationScores.getKey(),
                                          relatedAnnotationScores.getValue() );
                  }
               }
            }
         }
      } catch ( IOException ioE ) {
         throw new AnalysisEngineProcessException( ioE );
      }
   }

   static private void fillSectionsMaps( final JCas jCas,
                                 final Map<String,Collection<IdentifiedAnnotation>> uriAnnotationsMap,
                                 final Collection<Pair<Integer>> paragraphBounds,
                                         final Collection<Pair<Integer>> listEntryBounds,
                                         final Map<String,Sentence> sentences,
                                 final Map<Integer,Integer> tokenBeginToTokenNums,
                                 final Map<Integer,Integer> tokenEndToTokenNums ) {
      for ( IdentifiedAnnotation annotation : JCasUtil.select( jCas, IdentifiedAnnotation.class ) ) {
         Neo4jOntologyConceptUtil.getUris( annotation )
                                 .forEach( u -> uriAnnotationsMap
                                       .computeIfAbsent( u, a -> new HashSet<>() ).add( annotation ) );
      }
      if ( uriAnnotationsMap.size() < 2 ) {
         // Only a single uri, no relations can be made.
         return;
      }
      // Just initializes.  Probably not necessary but may save some time locking.
      UriInfoCache.getInstance().createDocUriNodeMap( uriAnnotationsMap.keySet() );
      // Paragraphs.  For confidence.  Todo - ctakes: add paragraphID to IdentifiedAnnotation.
      paragraphBounds.addAll( JCasUtil.select( jCas, Paragraph.class )
                                                                .stream()
                                                                .filter( Objects::nonNull )
                                                                .map( p -> new Pair<>( p.getBegin(), p.getEnd() ) )
                                                                .collect( Collectors.toList() ) );
      // ListEntries.  For confidence.
      listEntryBounds.addAll( JCasUtil.select( jCas, ListEntry.class )
                                      .stream()
                                      .filter( Objects::nonNull )
                                      .map( p -> new Pair<>( p.getBegin(), p.getEnd() ) )
                                      .collect( Collectors.toList() ) );
      JCasUtil.select( jCas, Sentence.class ).forEach( s -> sentences.put( ""+s.getSentenceNumber(), s ) );
      // Token indices.  For confidence.
      int i = 1;
      for ( BaseToken token : JCasUtil.select( jCas, BaseToken.class ) ) {
         tokenBeginToTokenNums.put( token.getBegin(), i );
         tokenEndToTokenNums.put( token.getEnd(), i );
         i++;
      }
   }

   static private final Collection<String> LOCATION_RELATIONS
         = new HashSet<>( Arrays.asList( HAS_LATERALITY, DISEASE_HAS_PRIMARY_ANATOMIC_SITE,
                               DISEASE_HAS_ASSOCIATED_ANATOMIC_SITE ) );
//         , HAS_QUADRANT, HAS_CLOCKFACE ) );

   static private Collection<RelationScore> createAnnotationRelationScores(
         final String relationName,
         final double uriRelationScore,
                             final IdentifiedAnnotation sourceAnnotation,
                             final AnnotationPenalties sourcePenalties,
                             final Collection<IdentifiedAnnotation> targetAnnotations,
                             final Map<String,Sentence> sentences,
                             final Collection<Pair<Integer>> paragraphBounds,
                               final Collection<Pair<Integer>> listEntryBounds,
                               final Map<Integer,Integer> tokenBeginToTokenNums,
                             final Map<Integer,Integer> tokenEndToTokenNums,
                             final int docLength ) {
      final boolean isLocation = LOCATION_RELATIONS.contains( relationName );
      final Collection<RelationScore> relationScores = new HashSet<>();
      for ( IdentifiedAnnotation targetAnnotation : targetAnnotations ) {
         final AnnotationPenalties targetPenalties = createAnnotationPenalties( isLocation,
                                                                                targetAnnotation,
                                                                               sentences,
                                                                               tokenBeginToTokenNums,
                                                                               tokenEndToTokenNums,
                                                                               docLength );
         final Pair<Double> placementPenalty = getPlacementPenalty( sourceAnnotation, targetAnnotation,
                                                                    paragraphBounds, listEntryBounds );
         final double placementScore = getPlacementScore( sourcePenalties.getBeginEndTokens(),
                                                          targetPenalties.getBeginEndTokens(),
                                                          placementPenalty );
         if ( isLocation && placementScore <= 1 ) {
//            NeoplasmSummaryCreator.addDebug( "Discarding Placement Relation "
//                                             + Neo4jOntologyConceptUtil.getUri( sourceAnnotation )
//                                             + " " + relationName + " "
//                                             + Neo4jOntologyConceptUtil.getUri( targetAnnotation ) +
//                                             " " + placementScore + "\n" );
            continue;
         }
         final RelationScore targetScore = createRelationScore( isLocation,
                                                                targetAnnotation,
                                                              uriRelationScore,
                                                              placementScore,
                                                              sourcePenalties,
                                                              targetPenalties );
         if ( targetScore.getTotalScore() > 1 ) {
//            NeoplasmSummaryCreator.addDebug( "Keeping Target Relation " +  Neo4jOntologyConceptUtil.getUri( sourceAnnotation )
//                                             + " " + relationName + " " + Neo4jOntologyConceptUtil.getUri( targetAnnotation ) +
//                                             " " + targetScore.getTotalScore() + "\n" );
            relationScores.add( targetScore );
//         } else {
//            NeoplasmSummaryCreator.addDebug( "Discarding Target Relation " + Neo4jOntologyConceptUtil.getUri( sourceAnnotation )
//                                             + " " + relationName + " " + Neo4jOntologyConceptUtil.getUri( targetAnnotation ) +
//                                             " " + targetScore.getTotalScore() + "\n" );
         }
      }
      return relationScores;
   }

   private void createAllRelations( final JCas jCas,
                                     final IdentifiedAnnotation source,
                                     final String relationName,
                                     Collection<RelationScore> AnnotationScores ) {
      final RelationBuilder<BinaryTextRelation> builder
            = new RelationBuilder<>().creator( BinaryTextRelation::new )
                                     .name( relationName )
                                     .annotation( source )
                                     .discoveredBy( CONST.NE_DISCOVERY_TECH_EXPLICIT_AE );
      for ( RelationScore annotationScore : AnnotationScores ) {
         builder.hasRelated( annotationScore._targetAnnotation )
               .confidence( (float)annotationScore.getTotalScore() )
                .build( jCas );
//         final boolean sourceNegated = IdentifiedAnnotationUtil.isNegated( source );
//         final boolean targetNegated = IdentifiedAnnotationUtil.isNegated( annotationScore._targetAnnotation );
//         NeoplasmSummaryCreator.addDebug(  "CrRelationFinder.createAllRelations " + ( sourceNegated ? "-" : "+")
//                                           + Neo4jOntologyConceptUtil.getUri( source ) + " "
//                                 + source.getBegin() + "," + source.getEnd() + "  *"
//                                 + relationName + "*  " + (targetNegated?"-":"+")
//                                 + Neo4jOntologyConceptUtil.getUri( annotationScore._targetAnnotation ) + " "
//                                 + annotationScore._targetAnnotation.getBegin() + "," + annotationScore._targetAnnotation.getEnd()
//                                           + " = score listed above\n");
      }
   }





   static private class RelationScore {
      private final IdentifiedAnnotation _targetAnnotation;
      private final double _uriRelationScore;
      private final double _distanceScore;
      private final AnnotationPenalties _sourcePenalties;
      private final AnnotationPenalties _targetPenalties;
      private double _totalScore = Double.MIN_VALUE;

      private RelationScore( final IdentifiedAnnotation targetAnnotation,
                             final double uriRelationScore,
                             final double distanceScore,
                             final AnnotationPenalties sourcePenalties,
                             final AnnotationPenalties targetPenalties ) {
         _targetAnnotation = targetAnnotation;
         _uriRelationScore = uriRelationScore;
         _distanceScore = distanceScore;
         _sourcePenalties = sourcePenalties;
         _targetPenalties = targetPenalties;
      }
      protected double getMinimumScore() {
         return MIN_RELATION_CONFIDENCE;
      }
      /**
       *
       * @return between 1 and 100.  100 is max because the uri relation score and distance score have a max of 100.
       */
      private double getTotalScore() {
         if ( _totalScore != Double.MIN_VALUE ) {
            return _totalScore;
         }
         _totalScore = Math.max( getMinimumScore(), ( _uriRelationScore + _distanceScore ) / 2
                                             - _sourcePenalties.getTotalPenalty()
                                             - _targetPenalties.getTotalPenalty() );
//         NeoplasmSummaryCreator.addDebug( "CrRelationFinder.TargetAnnotationScore: (rType+placement)"
//                                          + "/2- ( " +
//                                          Neo4jOntologyConceptUtil.getUri( _targetAnnotation ) + "(" +
//                                          _uriRelationScore + "+" + _distanceScore + ")/2 - "
//                                          + _sourcePenalties  + " " + _targetPenalties
//                                          + " ) = " + _totalScore + "\n" );
         return _totalScore;
      }
   }


   static private class LocationRelationScore extends RelationScore {
      private LocationRelationScore( final IdentifiedAnnotation targetAnnotation,
                             final double uriRelationScore,
                             final double distanceScore,
                             final AnnotationPenalties sourcePenalties,
                             final AnnotationPenalties targetPenalties ) {
         super( targetAnnotation, uriRelationScore, distanceScore, sourcePenalties, targetPenalties );
      }
      protected double getMinimumScore() {
         return MIN_LOCATION_CONFIDENCE;
      }
   }




   /**
    *
    * @param source -
    * @param target -
    * @param placementPenalty -
    * @return score between 3 and 100.  Distances and penalties > 155 are 3.
    */
   static private double getPlacementScore(  final Pair<Integer> source, final Pair<Integer> target,
                                            final Pair<Double> placementPenalty) {
      final double tokenDistance = getDistance( source, target);
      if ( tokenDistance == 0 ) {
//         NeoplasmSummaryCreator.addDebug( "CrRelationFinder.getPlacementScore: 100\n" );
         return 100;
      }
//      NeoplasmSummaryCreator.addDebug( "CrRelationFinder.getPlacementScore: 100 - ("
//                                       + placementPenalty.getValue1() + " + " + placementPenalty.getValue2()
//                                       + " * " + tokenDistance + ") = " + MIN_PLACEMENT_SCORE + " or "
//                                       + (100 - (placementPenalty.getValue1()
//                                                 + placementPenalty.getValue2() * tokenDistance)) + "\n" );
      return Math.max( MIN_PLACEMENT_SCORE, 100 - (placementPenalty.getValue1() + placementPenalty.getValue2() * tokenDistance) );
   }

   static private int getBeginTokenNum( final int annotationBegin, final Map<Integer,Integer> tokenBeginToTokenNum ) {
      for ( int i=annotationBegin; i>=0; i-- ) {
         if ( tokenBeginToTokenNum.containsKey( i ) ) {
            return tokenBeginToTokenNum.get( i );
         }
      }
      return 0;
   }

   static private int getEndTokenNum( final int annotationEnd, final Map<Integer,Integer> tokenEndToTokenNum,
                                      final int docLength ) {
      for ( int i=annotationEnd; i<docLength; i++ ) {
         if ( tokenEndToTokenNum.containsKey( i ) ) {
            return tokenEndToTokenNum.get( i );
         }
      }
      return tokenEndToTokenNum.size()-1;
   }

   static private int getDistance( final int begin1, final int end1, final int begin2, final int end2 ) {
      if ( end1 < begin2 ) {
         return begin2 - end1;
      } else if ( end2 < begin1 ) {
         return begin1 - end2;
      }
      return 0;
   }

   static private int getDistance( final Pair<Integer> source, final Pair<Integer> target ) {
      return getDistance( source.getValue1(), source.getValue2(), target.getValue1(), target.getValue2() );
   }



   static private Pair<Double> getPlacementPenalty( final IdentifiedAnnotation source,
                                                    final IdentifiedAnnotation target,
                                                    final Collection<Pair<Integer>> paragraphBounds,
                                                    final Collection<Pair<Integer>> listEntryBounds ) {
      if ( !source.getSegmentID().equals( target.getSegmentID() ) ) {
         // Not in the same section, so nothing else can be in common.
         return new Pair<>( DOCUMENT_OFFSET, DOCUMENT_MULTIPLIER );
      }
      if ( source.getSentenceID().equals( target.getSentenceID() ) ) {
         return new Pair<>( SENTENCE_OFFSET, SENTENCE_MULTIPLIER );
      }
      if ( inSameListEntry( source, target, listEntryBounds ) ) {
         return new Pair<>( LIST_ENTRY_OFFSET, LIST_ENTRY_MULTIPLIER );
      }
      if ( inSameParagraph( source, target, paragraphBounds ) ) {
         return new Pair<>( PARAGRAPH_OFFSET, PARAGRAPH_MULTIPLIER );
      }
      // In the same Section, but that is the best of it.
      return new Pair<>( SECTION_OFFSET, SECTION_MULTIPLIER );
   }

   static private boolean inSameParagraph( final IdentifiedAnnotation source,
                                           final IdentifiedAnnotation target,
                                           final Collection<Pair<Integer>> paragraphBounds ) {
      for ( Pair<Integer> paragraph : paragraphBounds ) {
         final boolean foundSource = paragraph.getValue1() <= source.getBegin()
                                     && source.getEnd() <= paragraph.getValue2();
         final boolean foundTarget = paragraph.getValue1() <= target.getBegin()
                                     && target.getEnd() <= paragraph.getValue2();
         if ( foundSource != foundTarget ) {
            return false;
         } else if ( foundSource ) {
            return true;
         }
      }
      return false;
   }

   static private boolean inSameListEntry( final IdentifiedAnnotation source,
                                           final IdentifiedAnnotation target,
                                           final Collection<Pair<Integer>> listEntryBounds ) {
      int sourceIndex = -1;
      int targetIndex = -2;
      int index = 1;
      for ( Pair<Integer> listEntry : listEntryBounds ) {
         if ( listEntry.getValue1() <= source.getBegin()
              && source.getEnd() <= listEntry.getValue2() ) {
            sourceIndex = index;
         }
         if ( listEntry.getValue1() <= target.getBegin()
              && target.getEnd() <= listEntry.getValue2() ) {
            targetIndex = index;
         }
         if ( sourceIndex > 0 && targetIndex > 0 ) {
            break;
         }
         index++;
      }
      return sourceIndex == targetIndex;
   }

   static private double getAssertionPenalty( final IdentifiedAnnotation target ) {
      double penalty = 0;
      if ( IdentifiedAnnotationUtil.isNegated( target ) ) {
         penalty += NEGATED_PENALTY;
      }
      if ( IdentifiedAnnotationUtil.isUncertain( target ) ) {
         penalty += UNCERTAIN_PENALTY;
      }
      return penalty;
   }

   static private double getHistoryPenalty( final String precedingText, final IdentifiedAnnotation target ) {
      if ( isFamilyHistory( precedingText, target ) ) {
         return FAMILY_HISTORY_PENALTY;
      } else if ( isPersonalHistory( precedingText ) ) {
         return PERSONAL_HISTORY_PENALTY;
      }
      return 0;
   }


   static private double getTestPenalty( final String precedingText, final String sentenceText  ) {
//      if ( isTestSentence( precedingText ) ) {
      if ( isTestSentence( sentenceText ) ) {
         return TEST_PENALTY;
      }
      return 0;
   }

   static private final Collection<String> MASS_BUMPS = Arrays.asList(
         "tumor site", "primary tumor", "histologic type" );

   static private double getMassPenalty( final String precedingText, final String sentenceText ) {
      if ( MASS_BUMPS.stream().anyMatch( precedingText::contains ) ) {
         // boost
         return MASS_BUMP;
      }
      if ( sentenceText.contains( "node" ) || sentenceText.contains( "nodul" ) ) {
         return NODE_PENALTY;
      }
      if ( isMassSentence( precedingText, sentenceText ) ) {
         return MASS_PENALTY;
      }
      return 0;
   }


   static private double getNearbyPenalty( final String precedingText ) {
      if ( isIndirectSite( precedingText ) ) {
         return NEARBY_PENALTY;
      }
      return 0;
   }

   static private final Collection<String> UNWANTED_SITE_PRECEDENTS = new HashSet<>( Arrays.asList(
         "near ",
         "over ",
         "under ",
         "above ",
         "below ",
         "between ",
//         "in "
         // Often "metastasis to the"
         "from ",
         "to the ",
         "adjacent to ",
         "adj to ",
         "anterior to ",
         "superior to "
                                                                                                  ) );

   static private boolean isIndirectSite( final String precedingText ) {
      for ( String word : UNWANTED_SITE_PRECEDENTS ) {
         if ( precedingText.endsWith( word ) || precedingText.endsWith( word + "the " ) ) {
            return true;
         }
      }
      return false;
   }

   static private final Collection<String> FAMILY_HISTORY_PRECEDENTS = new HashSet<>( Arrays.asList(
         "family history",
         "family hist",
         "family hx",
         "fam hx",
         "famhx",
         "fmhx",
         "fmh",
         "fh"
                                                                                                   ) );

   static private final Collection<String> PERSONAL_HISTORY_PRECEDENTS = new HashSet<>( Arrays.asList(
         "history of",
         "hist of",
         "hx"
                                                                                                   ) );

   static private boolean isFamilyHistory( final String precedingText, final IdentifiedAnnotation annotation ) {
      if ( !annotation.getSubject().equals( CONST.ATTR_SUBJECT_PATIENT ) ) {
         return true;
      }
      return FAMILY_HISTORY_PRECEDENTS.stream().anyMatch( precedingText::contains );
   }

   static private boolean isPersonalHistory( final String precedingText ) {
      return PERSONAL_HISTORY_PRECEDENTS.stream().anyMatch( precedingText::contains );
   }

   static private final Collection<String> TEST_PRECEDENTS = new HashSet<>( Arrays.asList(
         "specimen",
         "tissue",
         "biopsy",
         "biopsies",
         "bx",
         "fna",
         " ct",
         " cxr",
         "ray",
         " mri",
         " pet ",
         " pet:",
         "radio",
         "imaging",
         "ultrasound",
         "scan",
         " pe ",
         " pe:",
         "radiation",
         "view",
         "procedure",
         "frozen",
         "slide",
         "block",
         "cytology",
         "stain",
         "wash",
         "fluid",
         "exam",
         "screen",
         "mast",
         "ectomy",
         "pe:",
         "resect",
         "complaint",
         "excis",
         "incis",
         "source",
         "submit",
         "reveal",
         "seen",
         "presents",
         "evaluat",
         "perform",
         "thin"
                                                                                         ));

   // TODO When tests/procedures are added to the ontology use them here.
   static private boolean isTestSentence( final String precedingText ) {
      return TEST_PRECEDENTS.stream().anyMatch( precedingText::contains );
   }

   // This is probably important for location and laterality confidence.
   // TODO - Don't change confidence for source cancer ?
   static private final Collection<String> MASS_WORDS = new HashSet<>( Arrays.asList(
         " mass ",
         " mass.",
         " mass,",
         "masses",
         "metastas",
         "mets",
         "infiltrate",
         "infiltration",
         "implant",
         "cyst ",
         "polyp",
         "adnexal",
         "nodule",
         "node",
         "nodal",
         " ln",
         "sln",
         "lesion",
         "ascites",
         "density",
         "body",
         "bodies",
         "tiss",
         "cm ",
         "cm,",
         "cm.",
         "mm ",
         "mm,",
         "mm.",
         "largest",
         "not involved",
         "benign",
         "unremarkable",
         "margin"
                                                                                           ));

   static private boolean isMassSentence( final String precedingText, final String sentenceText ) {
      return MASS_WORDS.stream().anyMatch( sentenceText::contains );
   }

   static private final Collection<SectionType> PREFERRED_SECTIONS = new HashSet<>( Arrays.asList(
         FinalDiagnosis, ClinicalHistory, PreOpDiagnosis, PostOpDiagnosis, PrincipalDiagnosis, ClinicalData
                                                                                                ) );
   static private final Collection<SectionType> LESSER_SECTIONS = new HashSet<>( Arrays.asList(
         PriorTherapy, ReviewSystems, PastSurgicalHistory, HistologySummary,
         CurrentTherapy, FamilyHistory
                                                                                                 ) );
   // Not in ctakes, in dphe
   static private final Collection<String> LESSER_SECTION_NAMES =new HashSet<>( Arrays.asList(
         "specimen information", "microscopic description", "biopsy results" ) );

   static private final Collection<SectionType> NEOPLASM_LESSER_SECTIONS = new HashSet<>( Arrays.asList(
         Microscopic, ClinicalInfo
                                                                                              ) );
   // Not in ctakes, in dphe
//   static private final Collection<String> NEOPLASM_LESSER_SECTION_NAMES = new HashSet<>( Arrays.asList(
//         "specimen information" ) );

//   static private final Collection<SectionType> LOCATION_LESSER_SECTIONS = new HashSet<>( Arrays.asList() );
   // Not in ctakes, in dphe
//   static private final Collection<String> LOCATION_LESSER_SECTION_NAMES = new HashSet<>( Arrays.asList() );

   static private double getSectionPenalty( final IdentifiedAnnotation annotation ) {
      final SectionType sectionType = getSectionType( annotation );
      if ( PREFERRED_SECTIONS.contains( sectionType ) ) {
         return PREFERRED_SECTION_BUMP;
      }
      if ( LESSER_SECTIONS.contains( sectionType )
           || LESSER_SECTION_NAMES.contains( getSectionId( annotation ).toLowerCase() ) ) {

         return LESSER_SECTION_PENALTY;
      }
      return 0;
   }

   static private double getLengthPenalty( final IdentifiedAnnotation annotation ) {
      if ( annotation.getCoveredText().length() <= 3 ) {
         return SMALL_LENGTH_PENALTY;
      }
      return 0;
   }

   static private double getNeoplasmSectionPenalty( final IdentifiedAnnotation annotation ) {
      final SectionType sectionType = getSectionType( annotation );
      if ( PREFERRED_SECTIONS.contains( sectionType ) ) {
         return PREFERRED_SECTION_BUMP;
      }
      if ( LESSER_SECTIONS.contains( sectionType ) || NEOPLASM_LESSER_SECTIONS.contains( sectionType )
      || LESSER_SECTION_NAMES.contains( getSectionId( annotation ).toLowerCase() ) ) {
         return LESSER_SECTION_PENALTY;
      }
      return 0;
   }

   static private double getLocationSectionPenalty( final IdentifiedAnnotation annotation ) {
      final SectionType sectionType = getSectionType( annotation );
      if ( PREFERRED_SECTIONS.contains( sectionType ) ) {
         return PREFERRED_SECTION_BUMP;
      }
      if ( LESSER_SECTIONS.contains( sectionType ) || NEOPLASM_LESSER_SECTIONS.contains( sectionType )
           || LESSER_SECTION_NAMES.contains( getSectionId( annotation ).toLowerCase() ) ) {
         return LESSER_SECTION_PENALTY;
      }
      return 0;
   }

   static private SectionType getSectionType( final IdentifiedAnnotation annotation ) {
      final String sectionId = getSectionId( annotation );
      final SectionType sectionType = SectionType.getSectionType( sectionId );
//      NeoplasmSummaryCreator.addDebug( "Section: " + sectionType.getName() + " "
//                                       + sectionId + " "
//                                       + Neo4jOntologyConceptUtil.getUri( annotation ) + "\n" );
      return sectionType;
   }

   static private String getSectionId( final IdentifiedAnnotation annotation ) {
      final String sectionId = annotation.getSegmentID();
      final int scoreIndex = sectionId.indexOf( '_' );
      if ( scoreIndex > 0 ) {
         return sectionId.substring( 0, scoreIndex );
      }
      return sectionId;
   }


   static private Pair<String> getSentenceText( final IdentifiedAnnotation annotation,
                                           final Map<String,Sentence> sentences ) {
      final String sentenceId = annotation.getSentenceID();
      final Sentence sentence = sentences.getOrDefault( sentenceId, null );
      if ( sentence == null ) {
//            LOGGER.warn( "No Sentence for Annotation " + annotation.getCoveredText() );
         return new Pair<>( "", "" );
      }
      final String sentenceText = sentence.getCoveredText()
                                          .toLowerCase();
      final int begin = annotation.getBegin() - sentence.getBegin();
      final String precedingText = begin <= 0 ? "" : sentenceText.substring( 0, begin );
      return new Pair<>( precedingText, sentenceText );
   }

   static private class AnnotationPenalties {
      private final Pair<Integer> _beginEndTokens;
      private final double _assertionPenalty;
      private final double _historyPenalty;
      private final double _sectionPenalty;
      private AnnotationPenalties( final IdentifiedAnnotation annotation,
                                   final Map<String,Sentence> sentences,
                                   final Map<Integer,Integer> tokenBeginToTokenNums,
                                   final Map<Integer,Integer> tokenEndToTokenNums,
                                   final int docLength ) {
         final int beginToken = getBeginTokenNum( annotation.getBegin(),
                                                  tokenBeginToTokenNums );
         final int endToken = getEndTokenNum( annotation.getEnd(), tokenEndToTokenNums,
                                              docLength );
         _beginEndTokens = new Pair<>( beginToken, endToken );
         _assertionPenalty = getAssertionPenalty( annotation );
         _historyPenalty = getHistoryPenalty( getSentenceText(  annotation, sentences ).getValue1(), annotation );
         _sectionPenalty = getMySectionPenalty( annotation );
      }
      protected double getMySectionPenalty( final IdentifiedAnnotation annotation ) {
         return getSectionPenalty( annotation );
      }
      private Pair<Integer> getBeginEndTokens() {
         return _beginEndTokens;
      }
      protected double getTotalPenalty() {
         return _assertionPenalty + _historyPenalty + _sectionPenalty;
      }
      public String toString() {
         return " Assert " + _assertionPenalty + " Hist " + _historyPenalty + " Sect " + _sectionPenalty;
      }
   }

   static private class NeoplasmAnnotationPenalties extends AnnotationPenalties {

      private NeoplasmAnnotationPenalties( final IdentifiedAnnotation annotation,
                                           final Map<String, Sentence> sentences,
                                           final Map<Integer, Integer> tokenBeginToTokenNums,
                                           final Map<Integer, Integer> tokenEndToTokenNums, final int docLength ) {
         super( annotation, sentences, tokenBeginToTokenNums, tokenEndToTokenNums, docLength );
      }
      protected double getMySectionPenalty( final IdentifiedAnnotation annotation ) {
         return getNeoplasmSectionPenalty( annotation );
      }
   }


   static private class LocationAnnotationPenalties extends AnnotationPenalties {
      private final double _procedurePenalty;
      private final double _massPenalty;
      private final double _nearbyPenalty;
      private final double _lengthPenalty;
      private LocationAnnotationPenalties( final IdentifiedAnnotation annotation,
                                   final Map<String,Sentence> sentences,
                                   final Map<Integer,Integer> tokenBeginToTokenNums,
                                   final Map<Integer,Integer> tokenEndToTokenNums,
                                   final int docLength ) {
         super( annotation, sentences, tokenBeginToTokenNums, tokenEndToTokenNums, docLength );
         final Pair<String> sentenceText = getSentenceText( annotation, sentences );
         _procedurePenalty = getTestPenalty( sentenceText.getValue1(), sentenceText.getValue2() );
         _massPenalty = getMassPenalty( sentenceText.getValue1(), sentenceText.getValue2() );
         _nearbyPenalty = getNearbyPenalty( sentenceText.getValue1() );
         _lengthPenalty = getLengthPenalty( annotation );
      }
      protected double getMySectionPenalty( final IdentifiedAnnotation annotation ) {
         return getLocationSectionPenalty( annotation );
      }
      protected double getTotalPenalty() {
         return super.getTotalPenalty() + _procedurePenalty + _massPenalty + _nearbyPenalty + _lengthPenalty;
      }
      public String toString() {
         return super.toString() + " Proc " + _procedurePenalty + " Mass "
                + _massPenalty + " Near " + _nearbyPenalty
                 + " Length " + _lengthPenalty;
      }
   }


   static private AnnotationPenalties createAnnotationPenalties( final boolean isLocation,
                                                                 final IdentifiedAnnotation annotation,
                                                                 final Map<String,Sentence> sentences,
                                                                 final Map<Integer,Integer> tokenBeginToTokenNums,
                                                                 final Map<Integer,Integer> tokenEndToTokenNums,
                                                                 final int docLength) {
      return isLocation ? new LocationAnnotationPenalties( annotation,
                                                          sentences,
                                                          tokenBeginToTokenNums,
                                                          tokenEndToTokenNums,
                                                          docLength )
                       : new AnnotationPenalties( annotation,
                                                  sentences,
                                                  tokenBeginToTokenNums,
                                                  tokenEndToTokenNums,
                                                  docLength );
   }


   static private RelationScore createRelationScore( final boolean isLocation,
                                                     final IdentifiedAnnotation targetAnnotation,
                                                     final double uriRelationScore,
                                                     final double placementScore,
                                                     final AnnotationPenalties sourcePenalties,
                                                     final AnnotationPenalties targetPenalties ) {
      return isLocation ? new LocationRelationScore( targetAnnotation,
                                             uriRelationScore,
                                             placementScore,
                                             sourcePenalties,
                                             targetPenalties )
                        : new RelationScore( targetAnnotation,
                                               uriRelationScore,
                                               placementScore,
                                               sourcePenalties,
                                               targetPenalties );
   }

}
