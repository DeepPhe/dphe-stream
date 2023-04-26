package org.healthnlp.deepphe.summary.attribute.cr.stage;

import org.healthnlp.deepphe.neo4j.constant.Neo4jConstants;
import org.healthnlp.deepphe.summary.attribute.cr.newInfoStore.AbstractAttributeNormalizer;
import org.healthnlp.deepphe.summary.attribute.cr.newInfoStore.AttributeInfoCollector;
import org.healthnlp.deepphe.summary.engine.NeoplasmSummaryCreator;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * @author SPF , chip-nlp
 * @since {3/24/2023}
 */
public class StageNormalizer extends AbstractAttributeNormalizer {

   public String getBestCode( final AttributeInfoCollector infoCollector ) {
      return getBestTextCode( infoCollector.getAllRelations() );
   }


   public String getDefaultTextCode() {
      return "";
   }

//   public String getBestCode( final Collection<CrConceptAggregate> aggregates ) {
//      if ( aggregates.isEmpty() ) {
//         // No Default stage.
//         return "";
//      }
//      final Map<String,Long> countMap = createCodeCountMap( aggregates );
//      final List<String> codeList = new ArrayList<>( countMap.keySet() );
//      codeList.sort( Comparator.comparingInt( SORT_LIST::indexOf ).reversed() );
//      final String bestCode = codeList.get( 0 );
//      if ( bestCode.isEmpty() ) {
//         return "";
//      }
//      long bestCount = countMap.get( bestCode );
//      setBestCodesCount( (int)bestCount );
//      setAllCodesCount( aggregates.size() );
//      setUniqueCodeCount( countMap.size() );
//      NeoplasmSummaryCreator.addDebug( "StageNormalizer "
//                                       + countMap.entrySet().stream()
//                                                 .map( e -> e.getKey() + ":" + e.getValue() )
//                                                 .collect( Collectors.joining(",") ) + " = "
//                                       + bestCode +"\n");
//      return bestCode;
//   }

   protected void sortTextCodes( final List<String> codes ) {
      codes.sort( Comparator.comparingInt( SORT_LIST::indexOf ).reversed() );
   }


   public String getTextCode( final String uri ) {
      if ( uri.isEmpty() || uri.equals( Neo4jConstants.MISSING_NODE_NAME ) ) {
         return "";
      }
      if ( uri.equals( "Locally_Metastatic" ) ) {
         return "III";
      }
      if ( uri.equals( "Metastatic" ) || uri.equals( "Distantly_Metastatic" ) ) {
         return "IV";
      }
      if ( uri.length() == 7 ) {
         switch ( uri ) {
            case "In_Situ":
               return "0";
            case "Stage_0":
               return "0";
            case "Stage_1":
               return "I";
            case "Stage_2":
               return "II";
            case "Stage_3":
               return "III";
            case "Stage_4":
               return "IV";
            case "Stage_5":
               return "V";
         }
      }
      if ( uri.length() < 8 ) {
         NeoplasmSummaryCreator.addDebug( "BAD STAGE? " + uri + "\n" );
         return "Not Found";
      }
      final String subUri = uri.substring( 0, 8 );
      switch ( subUri ) {
         case "Stage_Un":
            return "Not Found";
         case "Stage_0_":
            return "0";
         case "Stage_0i":
            return "0";
         case "Stage_0a":
            return "0";
         case "Stage_Is":
            return "0";
         case "Stage_1_":
            return "I";
         case "Stage_1m":
            return "I";
         case "Stage_1A":
            return "IA";
         case "Stage_1B":
            return "IB";
         case "Stage_1C":
            return "IC";
         case "Stage_2_":
            return "II";
         case "Stage_2A":
            return "IIA";
         case "Stage_2B":
            return "IIB";
         case "Stage_2C":
            return "IIC";
         case "Stage_3_":
            return "III";
         case "Stage_3A":
            return "IIIA";
         case "Stage_3B":
            return "IIIB";
         case "Stage_3C":
            return "IIIC";
         case "Stage_4_":
            return "IV";
         case "Stage_4A":
            return "IVA";
         case "Stage_4B":
            return "IVB";
         case "Stage_4C":
            return "IVC";
         case "Stage_5_":
            return "V";
      }
      return "";
   }

   static private final List<String> SORT_LIST = Arrays.asList( "", "0", "I", "IA", "IB", "IC",
                                                 "II", "IIA", "IIB", "IIC",
                                                 "III", "IIIA", "IIIB", "IIIC",
                                                 "IV", "IVA", "IVB", "IVC", "V" );

   protected void fillEvidenceMap( final AttributeInfoCollector infoCollector, final Map<String,String> dependencies) {
      useAllEvidenceMap( infoCollector, dependencies );
   }

}
