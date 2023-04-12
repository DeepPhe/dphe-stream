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
import org.apache.ctakes.typesystem.type.textspan.Paragraph;
import org.apache.log4j.Logger;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.healthnlp.deepphe.core.neo4j.Neo4jOntologyConceptUtil;
import org.healthnlp.deepphe.neo4j.constant.RelationConstants;
import org.healthnlp.deepphe.nlp.uri.UriInfoCache;
import org.healthnlp.deepphe.summary.engine.NeoplasmSummaryCreator;

import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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

   static private final Pattern WHITESPACE = Pattern.compile( "\\s+" );




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

         // All Identified Annotations.
         final Map<String,Collection<IdentifiedAnnotation>> uriAnnotationsMap = new HashMap<>();
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
         final Map<String, UriInfoCache.UriNode> docUriNodeMap
               = UriInfoCache.getInstance().createDocUriNodeMap( uriAnnotationsMap.keySet() );

         // Paragraphs.  For confidence.  Todo - add paragraphID to IdentifiedAnnotation.
         final Collection<Pair<Integer>> paragraphBounds = JCasUtil.select( jCas, Paragraph.class )
                                                                   .stream()
                                                                   .filter( Objects::nonNull )
                                                                   .map( p -> new Pair<>( p.getBegin(), p.getEnd() ) )
                                                                   .collect( Collectors.toList() );
         // Token indices.  For confidence.
         final Map<Integer,Integer> tokenBeginToTokenNums = new HashMap<>();
         final Map<Integer,Integer> tokenEndToTokenNums = new HashMap<>();
         int i = 1;
         for ( BaseToken token : JCasUtil.select( jCas, BaseToken.class ) ) {
            tokenBeginToTokenNums.put( token.getBegin(), i );
            tokenEndToTokenNums.put( token.getEnd(), i );
            i++;
         }
         final String docText = jCas.getDocumentText();
         final int docLength = docText.length();

         // Loop through URIs and Annotations.
         for ( Map.Entry<String,Collection<IdentifiedAnnotation>> uriSourceAnnotations : uriAnnotationsMap.entrySet() ) {
            final String sourceUri = uriSourceAnnotations.getKey();
            final UriInfoCache.UriNode sourceNode = UriInfoCache.getInstance().getUriNode( sourceUri );
            if ( sourceNode.lacksRelations() ) {
               // Move on to next source URI.
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
                  final Map<String,Collection<TargetAnnotationScore>> relatedAnnotationScoresMap = new HashMap<>();
                  final double sourceAssertionPenalty = getAssertionPenalty( sourceAnnotation );
                  for ( IdentifiedAnnotation targetAnnotation : uriTargetAnnotations.getValue() ) {
                     final double targetAssertionPenalty = getAssertionPenalty( targetAnnotation );
                     for ( Map.Entry<String,Double> relationScores : relationScoresMap.entrySet() ) {
                        final String relationName = relationScores.getKey();
                        if ( (relationName.equals( RelationConstants.DISEASE_HAS_ASSOCIATED_ANATOMIC_SITE )
                              || relationName.equals( RelationConstants.DISEASE_HAS_PRIMARY_ANATOMIC_SITE ) )
                             && !isDirectSite( docText, targetAnnotation ) ) {
                           continue;
                        }
                        final double uriRelationScore = relationScores.getValue();
                        final double distanceScore = getPlacementScore( sourceAnnotation, targetAnnotation,
                                                                       tokenBeginToTokenNums,
                                                                       tokenEndToTokenNums,
                                                                       paragraphBounds, docLength );
                        final TargetAnnotationScore targetScore = new TargetAnnotationScore( targetAnnotation,
                                                                                            uriRelationScore,
                                                                                            distanceScore,
                                                                                            sourceAssertionPenalty,
                                                                                            targetAssertionPenalty );
                        relatedAnnotationScoresMap.computeIfAbsent( relationName, r -> new HashSet<>() )
                                               .add( targetScore );
                     }
                  }
                  for ( Map.Entry<String,Collection<TargetAnnotationScore>> relatedAnnotationScores
                        : relatedAnnotationScoresMap.entrySet() ) {
//                     LOGGER.info( sourceAnnotation.getCoveredText() + " "
//                                  + sourceAnnotation.getBegin() + "," + sourceAnnotation.getEnd() + " "
//                                  + relatedAnnotationScores.getKey() );
//                     for ( TotalAnnotationScore annotationScore : relatedAnnotationScores.getValue() ) {
//                        LOGGER.info( "   " + annotationScore._annotation.getCoveredText() + " "
//                                     + annotationScore._annotation.getBegin() + ","
//                                     + annotationScore._annotation.getEnd() + " : "
//                                     + annotationScore._uriScore + " + "
//                                     + annotationScore._distanceScore + " = "
//                                     + annotationScore.getTotalScore() );
//                     }
//                     createBestRelations( jCas, sourceAnnotation,
//                                          relatedAnnotationScores.getKey(),
//                                          relatedAnnotationScores.getValue() );
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

   private void createAllRelations( final JCas jCas,
                                     final IdentifiedAnnotation source,
                                     final String relationName,
                                     Collection<TargetAnnotationScore> AnnotationScores ) {
      final RelationBuilder<BinaryTextRelation> builder
            = new RelationBuilder<>().creator( BinaryTextRelation::new )
                                     .name( relationName )
                                     .annotation( source )
                                     .discoveredBy( CONST.NE_DISCOVERY_TECH_EXPLICIT_AE );
      for ( TargetAnnotationScore annotationScore : AnnotationScores ) {
         builder.hasRelated( annotationScore._annotation )
               .confidence( (float)annotationScore.getTotalScore() )
                .build( jCas );
         final boolean sourceNegated = IdentifiedAnnotationUtil.isNegated( source );
         final boolean targetNegated = IdentifiedAnnotationUtil.isNegated( annotationScore._annotation );
         NeoplasmSummaryCreator.addDebug(  "CrRelationFinder.createAllRelations " + ( sourceNegated ? "-" : "+") + source.getCoveredText() + " "
                                 + source.getBegin() + "," + source.getEnd() + "  *"
                                 + relationName + "*  " + (targetNegated?"-":"+")
                                 + annotationScore._annotation.getCoveredText() + " "
                                 + annotationScore._annotation.getBegin() + "," + annotationScore._annotation.getEnd()
                                           + " = score listed above\n");
      }
   }


//   private void createBestRelations( final JCas jCas,
//                                     final IdentifiedAnnotation source,
//                                     final String relationName,
//                                     Collection<TotalAnnotationScore> AnnotationScores ) {
//      final RelationBuilder<BinaryTextRelation> builder
//            = new RelationBuilder<>().creator( BinaryTextRelation::new )
//                                     .annotation( source )
//                                     .discoveredBy( CONST.NE_DISCOVERY_TECH_EXPLICIT_AE );
//      double bestScore = 0d;
//      final Collection<IdentifiedAnnotation> bestTargets = new HashSet<>();
//      double nextScore = 0d;
//      final Collection<IdentifiedAnnotation> nextTargets = new HashSet<>();
//      LOGGER.info( "Relation: " + relationName );
//      for ( TotalAnnotationScore annotationScore : AnnotationScores ) {
//         final double totalScore = annotationScore.getTotalScore();
////         LOGGER.info( "   " + source.getCoveredText() + " "
////                      + source.getBegin() + "," + source.getEnd() + " "
////                      + relationName + " "
////                      + annotationScore._annotation.getCoveredText() + " "
////                      + annotationScore._annotation.getBegin() + "," + annotationScore._annotation.getEnd() + " "
////                      + annotationScore._uriScore + " + " + annotationScore._distanceScore + " = " + totalScore );
//         if ( totalScore >= bestScore ) {
//            if ( totalScore > bestScore ) {
//               nextTargets.clear();
//               nextTargets.addAll( bestTargets );
//               bestTargets.clear();
//               bestScore = totalScore;
//            }
//            bestTargets.add( annotationScore._annotation );
//         } else if ( totalScore >= nextScore ) {
//            if ( totalScore > nextScore ) {
//               nextTargets.clear();
//               nextScore = totalScore;
//            }
//            nextTargets.add( annotationScore._annotation );
//         }
//      }
//      builder.name( relationName ).confidence( (float)bestScore );
//      for ( IdentifiedAnnotation target : bestTargets ) {
//         builder.hasRelated( target ).build( jCas );
//         LOGGER.info( "      Best Relation " + source.getCoveredText() + " "
//                      + source.getBegin() + "," + source.getEnd() + " "
//                      + relationName + " "
//                      + target.getCoveredText() + " "
//                      + target.getBegin() + "," + target.getEnd() + " = "
//                      + bestScore );
//      }
////      LOGGER.info( "         Best Target Count: " + bestTargets.size() );
//      if ( bestTargets.size() > 1 ) {
////            return;
//      }
//      builder.confidence( (float)nextScore );
//      for ( IdentifiedAnnotation target : nextTargets ) {
//         builder.hasRelated( target ).build( jCas );
//         LOGGER.info( "      Next Relation " + source.getCoveredText() + " "
//                      + source.getBegin() + "," + source.getEnd() + " "
//                      + relationName + " "
//                      + target.getCoveredText() + " "
//                      + target.getBegin() + "," + target.getEnd() + " = "
//                      + nextScore );
//      }
////      LOGGER.info( "         Next Target Count: " + nextTargets.size() );
//   }



   static private final double MIN_RELATION_CONFIDENCE = 1;

   static private class TargetAnnotationScore {
      private final IdentifiedAnnotation _annotation;
      private final double _uriRelationScore;
      private final double _distanceScore;
      private final double _sourceAssertionPenalty;
      private final double _targetAssertionPenalty;
      private TargetAnnotationScore( final IdentifiedAnnotation annotation, final double uriRelationScore,
                                     final double distanceScore, final double sourceAssertionPenalty,
                                     final double targetAssertionPenalty ) {
         _annotation = annotation;
         _uriRelationScore = uriRelationScore;
         _distanceScore = distanceScore;
         _sourceAssertionPenalty = sourceAssertionPenalty;
         _targetAssertionPenalty = targetAssertionPenalty;
      }

      /**
       *
       * @return between 1 and 100.  100 is max because the uri relation score and distance score have a max of 100.
       */
      private double getTotalScore() {
         NeoplasmSummaryCreator.addDebug( "CrRelationFinder.TargetAnnotationScore: (rType+placement)"
                                          + "/2-sourceAssert-targetAssert: "
                                          + _annotation.getCoveredText() +  "(" +
                                          _uriRelationScore + "+" + _distanceScore + ")/2 - "
                                          + _sourceAssertionPenalty + "-" + _targetAssertionPenalty +
                                          " = " + (( _uriRelationScore + _distanceScore ) / 2 - _sourceAssertionPenalty - _targetAssertionPenalty) +"\n" );
         return Math.max( MIN_RELATION_CONFIDENCE, ( _uriRelationScore + _distanceScore ) / 2 - _sourceAssertionPenalty - _targetAssertionPenalty );
      }
   }


   static private final double MIN_PLACEMENT_SCORE = 3;

   /**
    *
    * @param source -
    * @param target -
    * @param tokenBeginToTokenNum -
    * @param tokenEndToTokenNum -
    * @param paragraphBounds collection of paragraph char index bounds.
    * @return score between 3 and 100.  Distances and penalties > 155 are 3.
    */
   static private double getPlacementScore( final IdentifiedAnnotation source,
                                            final IdentifiedAnnotation target,
                                            final Map<Integer,Integer> tokenBeginToTokenNum,
                                            final Map<Integer,Integer> tokenEndToTokenNum,
                                            final Collection<Pair<Integer>> paragraphBounds,
                                            final int docLength ) {
      final double tokenDistance = getTokenDistance( source, target, tokenBeginToTokenNum, tokenEndToTokenNum, docLength );
      if ( tokenDistance == 0 ) {
//         NeoplasmSummaryCreator.addDebug( "CrRelationFinder.getPlacementScore: 0 distance!\n" );
         return 100;
      }
      final Pair<Double> penalty = getSectionPenalty( source, target, paragraphBounds );
//      NeoplasmSummaryCreator.addDebug( "CrRelationFinder.getPlacementScore: 100 - ("
//                                       + penalty.getValue1() + " + " + penalty.getValue2()
//                                       + " * " + tokenDistance + ") = "
//                                       + (100 - (penalty.getValue1() + penalty.getValue2() * tokenDistance)) + "\n" );
      return Math.max( MIN_PLACEMENT_SCORE, 100 - (penalty.getValue1() + penalty.getValue2() * tokenDistance) );
   }

   /**
    *
    * @param source -
    * @param target -
    * @param tokenBeginToTokenNum -
    * @param tokenEndToTokenNum -
    * @return whole number (floor) distance >= 0, short distances are below 100, long can quickly reach 1000
    */
   static private double getTokenDistance( final IdentifiedAnnotation source,
                                        final IdentifiedAnnotation target,
                                        final Map<Integer,Integer> tokenBeginToTokenNum,
                                        final Map<Integer,Integer> tokenEndToTokenNum,
                                           final int docLength ) {
      final int sourceBeginToken = getBeginTokenNum( source.getBegin(), tokenBeginToTokenNum );
      final int sourceEndToken = getEndTokenNum( source.getEnd(), tokenEndToTokenNum, docLength );
//      final double sourceCenter = (sourceBeginToken + sourceEndToken)/2d;
      final int targetBeginToken = getBeginTokenNum( target.getBegin(), tokenBeginToTokenNum );
      final int targetEndToken = getEndTokenNum( target.getEnd(), tokenEndToTokenNum, docLength );
//      final double targetCenter = (targetBeginToken + targetEndToken)/2d;
//      NeoplasmSummaryCreator.addDebug( "CrRelationFinder.getTokenDistance source, target: "
//                                       + source.getCoveredText() + "," + target.getCoveredText() + " "
//                                       + sourceCenter + "," + targetCenter + " Diff = "
//                                       + Math.abs( sourceCenter - targetCenter ) +"\n" );
//      return Math.floor( Math.abs( sourceCenter - targetCenter ) );
      final double distance = getDistance( sourceBeginToken, sourceEndToken, targetBeginToken, targetEndToken );
//      NeoplasmSummaryCreator.addDebug( "CrRelationFinder.getTokenDistance source, target: "
//                                       + source.getCoveredText() + "," + target.getCoveredText() + " "
//                                       + sourceBeginToken + "," + sourceEndToken + " to "
//                                       + targetBeginToken + "," + targetEndToken + " Distance = "
//                                       + distance +"\n" );
      return distance;
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


   static private final double SENTENCE_OFFSET = 0;
   static private final double PARAGRAPH_OFFSET = 5;
   static private final double SECTION_OFFSET = 10;
   static private final double DOCUMENT_OFFSET = 15;
   static private final double SENTENCE_MULTIPLIER = 0.1;
   static private final double PARAGRAPH_MULTIPLIER = 0.2;
   static private final double SECTION_MULTIPLIER = 0.3;
   static private final double DOCUMENT_MULTIPLIER = .5;

   static private Pair<Double> getSectionPenalty( final IdentifiedAnnotation source,
                                                  final IdentifiedAnnotation target,
                                                  final Collection<Pair<Integer>> paragraphBounds ) {
      if ( !source.getSegmentID().equals( target.getSegmentID() ) ) {
         // Not in the same section, which is the largest window of interest.
         return new Pair<>( DOCUMENT_OFFSET, DOCUMENT_MULTIPLIER );
      }
      if ( source.getSentenceID().equals( target.getSentenceID() ) ) {
         // In the same sentence, no penalty.
         return new Pair<>( SENTENCE_OFFSET, SENTENCE_MULTIPLIER );
      }
      for ( Pair<Integer> paragraph : paragraphBounds ) {
         final boolean foundSource = paragraph.getValue1() <= source.getBegin()
                                     && paragraph.getValue2() <= source.getEnd();
         final boolean foundTarget = paragraph.getValue1() <= target.getBegin()
                                     && paragraph.getValue2() <= target.getEnd();
         if ( foundSource != foundTarget ) {
            // Not in the same Paragraph, but in the same Section.
            return new Pair<>( SECTION_OFFSET, SECTION_MULTIPLIER );
         } else if ( foundSource ) {
            // In the same paragraph.
            return new Pair<>( PARAGRAPH_OFFSET, PARAGRAPH_MULTIPLIER );
         }
      }
      // In the same Section.
      return new Pair<>( SECTION_OFFSET, SECTION_MULTIPLIER );
   }


//   static private double getDistanceScore( final IdentifiedAnnotation source,
//                                           final IdentifiedAnnotation target,
//                                           final Map<Integer,Integer> tokenBeginToTokenNum,
//                                           final Map<Integer,Integer> tokenEndToTokenNum,
//                                           final Collection<Pair<Integer>> paragraphBounds ) {
//      final double offsetScore = getOffsetScore( source, target, tokenBeginToTokenNum, tokenEndToTokenNum );
//      if ( offsetScore > 99 ) {
//         return offsetScore;
//      }
//      final double windowPenalty = getWindowPenalty( source, target,  paragraphBounds );
//      return Math.max( 0, offsetScore - windowPenalty );
//   }
//
//   static private double getOffsetScore( final IdentifiedAnnotation source,
//                                         final IdentifiedAnnotation target,
//                                         final Map<Integer,Integer> tokenBeginToTokenNum,
//                                         final Map<Integer,Integer> tokenEndToTokenNum ) {
//      final int sourceBeginToken = tokenBeginToTokenNum.get( source.getBegin() );
//      final int targetBeginToken = tokenBeginToTokenNum.get( target.getBegin() );
//      if ( sourceBeginToken == targetBeginToken ) {
//         // Overlap.
//         return 100;
//      }
//      final int sourceEndToken = tokenEndToTokenNum.get( source.getEnd() );
//      if ( sourceBeginToken < targetBeginToken ) {
//         // source begin is before the target in the text.
//         if ( sourceEndToken >= targetBeginToken ) {
//            // Overlap.
//            return 100;
//         }
//         return Math.max( 1, 100 - (targetBeginToken - sourceEndToken) );
//      }
//      // target begin is before the source in the text.
//      final int targetEndToken = tokenEndToTokenNum.get( target.getEnd() );
//      if ( targetEndToken >= sourceBeginToken ) {
//         // Overlap.
//         return 100;
//      }
//      return Math.max( 1, 100 - (sourceBeginToken - targetEndToken) );
//   }


   static private final double SENTENCE_PENALTY = 0;
   static private final double PARAGRAPH_PENALTY = 10;
   static private final double SECTION_PENALTY = 20;
   static private final double DOC_PENALTY = 30;

   static private double getWindowPenalty( final IdentifiedAnnotation source,
                                         final IdentifiedAnnotation target,
                                         final Collection<Pair<Integer>> paragraphBounds ) {
      if ( !source.getSegmentID().equals( target.getSegmentID() ) ) {
         // Not in the same section, which is the largest window of interest.
         return DOC_PENALTY;
      }
      if ( source.getSentenceID().equals( target.getSentenceID() ) ) {
         // In the same sentence, no penalty.
         return SENTENCE_PENALTY;
      }
      for ( Pair<Integer> paragraph : paragraphBounds ) {
         final boolean foundSource = paragraph.getValue1() <= source.getBegin()
              && paragraph.getValue2() <= source.getEnd();
         final boolean foundTarget = paragraph.getValue1() <= target.getBegin()
                                     && paragraph.getValue2() <= target.getEnd();
         if ( foundSource != foundTarget ) {
            // Not in the same Paragraph, but in the same Section.
            return SECTION_PENALTY;
         } else if ( foundSource ) {
            // In the same paragraph.
            return PARAGRAPH_PENALTY;
         }
      }
      // In the same Section.
      return SECTION_PENALTY;
   }

   static private final double NEGATED_PENALTY = 30;
   static private final double UNCERTAIN_PENALTY = 5;

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



   static private final Collection<String> UNWANTED_SITE_PRECEDENTS = new HashSet<>( Arrays.asList(
         "near ",
         "over ",
         "under ",
         "above ",
         "below ",
         "between ",
//         "in "
         // Often "metastasis to the"
         "to the ",
         "adjacent to ",
         "anterior to ",
         "superior to "
                                                                                                  ) );

   static private boolean isDirectSite( final String docText, final IdentifiedAnnotation site ) {
      final int begin = Math.max( 0, site.getBegin() - 40 );
      final String preceding = docText.substring( begin, site.getBegin() ).toLowerCase();
      return UNWANTED_SITE_PRECEDENTS.stream().noneMatch( preceding::contains );
   }










}
