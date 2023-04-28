package org.healthnlp.deepphe.summary.attribute.cr.laterality;

import org.healthnlp.deepphe.neo4j.constant.UriConstants;
import org.healthnlp.deepphe.nlp.uri.CustomUriRelations;
import org.healthnlp.deepphe.summary.attribute.cr.newInfoStore.AbstractAttributeNormalizer;
import org.healthnlp.deepphe.summary.attribute.cr.newInfoStore.AttributeInfoCollector;
import org.healthnlp.deepphe.summary.concept.ConceptAggregateRelation;
import org.healthnlp.deepphe.summary.engine.NeoplasmSummaryCreator;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author SPF , chip-nlp
 * @since {3/24/2023}
 */
public class LateralityNormalizer extends AbstractAttributeNormalizer {

   //  https://seer.cancer.gov/manuals/primsite.laterality.pdf
   //  Only the following major topographic sites can have an associated laterality.
   static private final Collection<String> LATERALITIES = new HashSet<>();
   static private final int[] FACILITY0 = new int[]{ 7, 8, 9 };
   static private final int[] FACILITY = new int[]{
         30, 31, 34, 38, 40, 41, 44, 47, 49, 50, 56, 57, 62, 63, 64, 65, 66, 69, 70, 71, 72, 74, 75
   };
   static {
      Arrays.stream( FACILITY0 ).forEach( n -> LATERALITIES.add( "C0" + n ) );
      Arrays.stream( FACILITY ).forEach( n -> LATERALITIES.add( "C" + n ) );
   }

//   static private final double CONFIDENCE_CUTOFF = 0.2;
//static private final double CONFIDENCE_CUTOFF = 0.1;
static private final double CONFIDENCE_CUTOFF = 1.5;

   private boolean _isLung = false;

   public void init( final AttributeInfoCollector infoCollector, final Map<String,String> dependencies ) {
      String topographyMajor = dependencies.getOrDefault( "topography_major", "" )
                                           .toUpperCase();
      if ( topographyMajor.length() > 3 ) {
         topographyMajor = topographyMajor.substring( 0, 3 );
      }
      final boolean hasLaterality = LATERALITIES.contains( topographyMajor );
      if ( !hasLaterality ) {
         // 0 is "Not a paired Site"
         setBestCode( "0" );
         fillEvidenceMap( infoCollector, dependencies );
//      } else if ( infoCollector.getConfidence() < CONFIDENCE_CUTOFF ) {
//         // A "Paired Site, but no confident information on laterality"
//         setBestCode( "9" );
//         fillEvidenceMap( infoCollector, dependencies );
      } else {
         _isLung = topographyMajor.equals( "C34" );
         super.init( infoCollector, dependencies );
      }
//      NeoplasmSummaryCreator.addDebug( "Laterality best = " + getBestCode() + " counts= " + getUniqueCodeCount() + "\n" );
   }

   public String getBestCode( final AttributeInfoCollector infoCollector ) {
      return getBestRelationCode( infoCollector.getAllRelations() );
   }


   public String getBestRelationCode( final Collection<ConceptAggregateRelation> relations ) {
      if ( relations.isEmpty() ) {
         return "9";
      }
      final Map<Integer,Double> intConfidenceMap = createIntCodeConfidenceMap( relations );
      NeoplasmSummaryCreator.addDebug( "LateralityNormalizer "
                                       + intConfidenceMap.entrySet().stream()
                                                    .map( e -> e.getKey() + ":" + e.getValue() )
                                                    .collect( Collectors.joining(",") )  +"\n");
      double rightConfidence = intConfidenceMap.getOrDefault( 1, -1d );
      double leftConfidence = intConfidenceMap.getOrDefault( 2, -1d );
      double unilateralConfidence = intConfidenceMap.getOrDefault( 3, -1d );
      double bilateralConfidence = intConfidenceMap.getOrDefault( 4, -1d );
      double unspecifiedConfidence = intConfidenceMap.getOrDefault( 9, -1d );

      if ( bilateralConfidence > 0 && leftConfidence > 0 && rightConfidence > 0 ) {
         if ( Math.min( leftConfidence, rightConfidence ) / Math.max( leftConfidence, rightConfidence ) > 0.8
              && bilateralConfidence / (leftConfidence + rightConfidence) > 0.2 ) {
            NeoplasmSummaryCreator.addDebug( "LateralityNormalizer Bilateral "
                                             + (Math.min( leftConfidence, rightConfidence ) / Math.max( leftConfidence,
                                                                                                        rightConfidence ) )
                                             + (bilateralConfidence / (leftConfidence + rightConfidence)) +"\n" );
            return 4+"";
         }
         if ( bilateralConfidence > leftConfidence || bilateralConfidence > rightConfidence ) {
            bilateralConfidence += Math.min( leftConfidence, rightConfidence );
         }
      }
      if ( bilateralConfidence > rightConfidence && bilateralConfidence > leftConfidence ) {
         NeoplasmSummaryCreator.addDebug( "LateralityNormalizer Bilateral by sum " + bilateralConfidence + "\n" );
         return 4+"";
      }
      if ( rightConfidence > leftConfidence && rightConfidence > unspecifiedConfidence ) {
         return 1+"";
      }
      if ( leftConfidence > unspecifiedConfidence ) {
         return 2+"";
      }
      if ( unilateralConfidence > unspecifiedConfidence ) {
         return 3+"";
      }
      return "9";
   }


   // https://seer.cancer.gov/archive/manuals/2021/SPCSM_2021_MainDoc.pdf
   public int getIntCode( final String uri ) {
       if ( uri.equals( UriConstants.BILATERAL ) ) {
         return 4;
      }
      if ( uri.equals( UriConstants.RIGHT ) || uri.equals( "Unilateral_Right" )
           || ( _isLung && CustomUriRelations.getInstance().getRightLungLateralityUris().contains( uri ) ) ) {
         return 1;
      }
      if ( uri.equals( UriConstants.LEFT ) || uri.equals( "Unilateral_Left" )
           || ( _isLung && CustomUriRelations.getInstance().getLeftLungLateralityUris().contains( uri ) ) ) {
         return 2;
      }
      if ( uri.equals( "Unilateral" ) ) {
         return 3;
      }
      if ( uri.equals( "Unspecified_Laterality" ) ) {
         return 9;
      }
      return -1;
   }

//   public Map<Integer,Double> createIntCodeConfidenceMap( final Collection<ConceptAggregateRelation> relations ) {
//      if ( !_isLung ) {
//         return super.createIntCodeConfidenceMap( relations );
//      }
//      final Map<Integer, List<ConceptAggregateRelation>> codeRelationsMap
//            = relations.stream()
//                       .collect( Collectors.groupingBy( this::getIntCode ) );
//      final Map<Integer,Double>  confidenceMap = new HashMap<>();
//      for ( Map.Entry<Integer,List<ConceptAggregateRelation>> codeRelations : codeRelationsMap.entrySet() ) {
//         NeoplasmSummaryCreator.addDebug( "AbstractAttributeNormalizer.createIntCodeConfidenceMap " +
//                                          codeRelations.getKey() + " "
//                                          + codeRelations.getValue().stream()
//                                                         .map( ConceptAggregateRelation::getTarget )
//                                                         .map( CrConceptAggregate::getUri )
//                                                         .collect( Collectors.joining(", ") ) + "\n");
//         NeoplasmSummaryCreator.addDebug( "AbstractAttributeNormalizer.createIntCodeConfidenceMap " +
//                                          codeRelations.getKey() + " "
//                                          + codeRelations.getValue().stream()
//                                                         .map( ConceptAggregateRelation::getConfidence )
//                                                         .map( c -> c+"" )
//                                                         .collect( Collectors.joining(", ") ) + "\n");
//         final double confidence = codeRelations.getValue()
//                                                .stream()
//                                                .mapToDouble( LateralityNormalizer::getMentionsConfidence )
//                                                .sum();
//         confidenceMap.put( codeRelations.getKey(), confidence );
//      }
//      return confidenceMap;
//   }
//
//   static private double getMentionsConfidence( final ConceptAggregateRelation relation ) {
//      final CrConceptAggregate target = relation.getTarget();
//      final Map<String,Mention> idMentions = target.getMentions()
//                                                 .stream()
//                                                 .collect( Collectors.toMap( Mention::getId, Function.identity() ) );
//      final Collection<MentionRelation> mentionRelations = relation.getMentionRelations();
//      final Collection<MentionRelation> sideRelations = new ArrayList<>();
//      for ( MentionRelation mentionRelation : mentionRelations ) {
//         final Mention mention = idMentions.get( mentionRelation.getTargetId() );
//         if ( mention != null
//              && CustomUriRelations.getInstance().getLateralityUris().contains( mention.getClassUri() ) ) {
//            sideRelations.add( mentionRelation );
//         }
//      }
//      if ( sideRelations.isEmpty() ) {
//         return 0;
//      }
//      return ConfidenceCalculator.calculateAggregateRelation( sideRelations );
//   }


}
