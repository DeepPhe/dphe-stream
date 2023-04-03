package org.healthnlp.deepphe.summary.attribute.cr.behavior;

import org.healthnlp.deepphe.core.neo4j.Neo4jOntologyConceptUtil;
import org.healthnlp.deepphe.summary.attribute.cr.newInfoStore.AbstractAttributeNormalizer;
import org.healthnlp.deepphe.summary.attribute.cr.newInfoStore.AttributeInfoCollector;
import org.healthnlp.deepphe.summary.concept.CrConceptAggregate;
import org.healthnlp.deepphe.summary.engine.NeoplasmSummaryCreator;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author SPF , chip-nlp
 * @since {3/23/2023}
 */
public class BehaviorNormalizer extends AbstractAttributeNormalizer {

   static private Collection<String> BENIGN_URIS;
   static private Collection<String> BORDERLINE_URIS;
   static private Collection<String> IN_SITU_URIS;
   static private Collection<String> MALIGNANT_URIS;
   static private Collection<String> METASTATIC_URIS;
   static private final int LYMPH_WINDOW = 30;
   static private final double LYMPH_REDUCTION = 2;


   public void init( final AttributeInfoCollector infoCollector, final Map<String,String> dependencies ) {
      if ( MALIGNANT_URIS == null ) {
         MALIGNANT_URIS = new HashSet<>( Neo4jOntologyConceptUtil.getBranchUris( "Invasive" ) );
         MALIGNANT_URIS.addAll( Neo4jOntologyConceptUtil.getBranchUris( "Malignant_Descriptor" ) );
         BENIGN_URIS = new HashSet<>( Neo4jOntologyConceptUtil.getBranchUris( "Benign" ) );
         BORDERLINE_URIS = new HashSet<>( Neo4jOntologyConceptUtil.getBranchUris( "Borderline" ) );
         BORDERLINE_URIS.addAll( Neo4jOntologyConceptUtil.getBranchUris( "Microinvasive_Tumor" ) );
         IN_SITU_URIS = new HashSet<>( Neo4jOntologyConceptUtil.getBranchUris( "In_Situ" ) );
         IN_SITU_URIS.addAll( Neo4jOntologyConceptUtil.getBranchUris( "Premalignant" ) );
         IN_SITU_URIS.addAll( Neo4jOntologyConceptUtil.getBranchUris( "Non_Malignant" ) );
         IN_SITU_URIS.addAll( Neo4jOntologyConceptUtil.getBranchUris( "Noninvasive" ) );
         METASTATIC_URIS = new HashSet<>( Neo4jOntologyConceptUtil.getBranchUris( "Metaplastic" ) );
         METASTATIC_URIS.addAll( Neo4jOntologyConceptUtil.getBranchUris( "Metastasis" ) );
         METASTATIC_URIS.addAll( Neo4jOntologyConceptUtil.getBranchUris( "Metastatic" ) );
      }
      super.init( infoCollector, dependencies );
      NeoplasmSummaryCreator.addDebug( "Behavior best = " + getBestCode() + " counts= " + getUniqueCodeCount() + "\n" );
   }

   public String getBestCode( final Collection<CrConceptAggregate> aggregates ) {
      if ( aggregates.isEmpty() ) {
         // The Cancer Registry default is 3.
         return "3";
      }

      final Map<Integer,Long> intCountMap = createIntCodeCountMap( aggregates );
      int bestCode = -1;
      long bestCodesCount = 0;
      for ( Map.Entry<Integer,Long> codeCount : intCountMap.entrySet() ) {
         final long count = codeCount.getValue();
         if ( count > bestCodesCount ) {
            bestCodesCount = count;
            bestCode = codeCount.getKey();
         } else if ( count == bestCodesCount ) {
            if ( bestCode == 3 || codeCount.getKey() == 3 ) {
               bestCode = 3;
            } else {
               bestCode = Math.max( bestCode, codeCount.getKey() );
            }
         }
      }
      setBestCodesCount( (int)bestCodesCount );
      setAllCodesCount( aggregates.size() );
      setUniqueCodeCount( intCountMap.size() );
      NeoplasmSummaryCreator.addDebug( "BehaviorNormalizer "
                                       + intCountMap.entrySet().stream()
                                                 .map( e -> e.getKey() + ":" + e.getValue() )
                                                 .collect( Collectors.joining(",") ) + " = "
                                       + bestCode +"\n");
      return bestCode < 0 ? "3" : bestCode+"";
   }

   private String getOppositeUri( final CrConceptAggregate aggregate ) {
      final String uri = aggregate.getUri();
      if ( METASTATIC_URIS.contains( uri ) ) {
         return "Borderline";
      } else if ( MALIGNANT_URIS.contains( uri ) ) {
         return "In_Situ";
      } else if ( IN_SITU_URIS.contains( uri ) ) {
         return "Invasive";
      } else if ( BORDERLINE_URIS.contains( uri ) ) {
         return "Benign";
      } else  if ( BENIGN_URIS.contains( uri ) ) {
         return "Invasive";
      }
      NeoplasmSummaryCreator.addDebug( "No Opposite Behavior code for " + uri +"\n");
      return uri;
   }

   public String getCode( final CrConceptAggregate aggregate ) {
      if ( aggregate.isNegated() ) {
         return getCode( getOppositeUri( aggregate ) );
      }
      return getCode( aggregate.getUri() );
   }


   public String getCode( final String uri ) {
      final int code = getIntCode( uri );
      return code < 0 ? "" : code+"";
   }

   protected int getIntCode( final String uri ) {
      if ( METASTATIC_URIS.contains( uri ) ) {
         return 3;
         // Cancer registries do not use behavior code 6
         //return 6;
      }
      if ( MALIGNANT_URIS.contains( uri ) ) {
         return 3;
      }
      if ( IN_SITU_URIS.contains( uri ) ) {
         return 2;
      }
      if ( BORDERLINE_URIS.contains( uri ) ) {
         return 1;
      }
      if ( BENIGN_URIS.contains( uri ) ) {
         return 0;
      }
      NeoplasmSummaryCreator.addDebug( "No Behavior code for " + uri +"\n");
      return -1;
   }


//   public double getConfidence() {
//      int lymphCount = 0;
//      for ( CrConceptAggregate aggregate : getAggregates() ) {
//         for ( Mention mention : aggregate.getMentions() ) {
//            final int mentionBegin = mention.getBegin();
//            if ( mentionBegin <= LYMPH_WINDOW ) {
//               continue;
//            }
//            final Note note = NoteNodeStore.getInstance()
//                                           .get( mention.getNoteId() );
//            if ( note == null ) {
////                  LOGGER.warn( "No Note stored for Note ID " + mention.getNoteId() );
//               continue;
//            }
//            final String preText = note.getText()
//                                       .substring( mentionBegin - LYMPH_WINDOW, mentionBegin )
//                                       .toLowerCase();
//            NeoplasmSummaryCreator.addDebug( "Behavior Candidate and pretext "
//                                             + note.getText()
//                                                   .substring( mentionBegin - LYMPH_WINDOW,
//                                                               mention.getEnd() )
//                                             + "\n" );
//            if ( preText.contains( "lymph node" ) ) {
//               NeoplasmSummaryCreator.addDebug( "Tracking Behavior uri "
//                                                + mention.getClassUri() + "\n" );
//               lymphCount++;
//            }
//         }
//      }
//      final double confidence = super.getRelations()
//                                     .stream()
//                                     .mapToDouble( ConceptAggregateRelation::getConfidence )
//                                     .average()
//                                     .orElse( 0 );
//      return Math.min( 10, confidence - LYMPH_REDUCTION*lymphCount );
//   }


}
