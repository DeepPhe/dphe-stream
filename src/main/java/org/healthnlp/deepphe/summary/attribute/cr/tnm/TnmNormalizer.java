package org.healthnlp.deepphe.summary.attribute.cr.tnm;

import org.healthnlp.deepphe.neo4j.constant.Neo4jConstants;
import org.healthnlp.deepphe.summary.attribute.cr.newInfoStore.AbstractAttributeNormalizer;
import org.healthnlp.deepphe.summary.attribute.cr.newInfoStore.AttributeInfoCollector;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * @author SPF , chip-nlp
 * @since {3/24/2023}
 */
public class TnmNormalizer extends AbstractAttributeNormalizer {

   public String getBestCode( final AttributeInfoCollector infoCollector ) {
      return getBestTextCode( infoCollector.getAllRelations() );
   }

   public String getDefaultTextCode() {
      return "";
   }


//   public String getBestCode( final Collection<CrConceptAggregate> aggregates ) {
//      if ( aggregates.isEmpty() ) {
//         // The Cancer Registry default is 9.
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
//      NeoplasmSummaryCreator.addDebug( "TnmNormalizer "
//                                       + countMap.entrySet().stream()
//                                                 .map( e -> e.getKey() + ":" + e.getValue() )
//                                                 .collect( Collectors.joining( "," ) ) + " = "
//                                       + bestCode + "\n");
//      return bestCode;
//   }

   protected void sortTextCodes( final List<String> codes ) {
      // By default use the highest code value.
      codes.sort( Comparator.comparingInt( SORT_LIST::indexOf ).reversed() );
   }


   public String getTextCode( final String uri ) {
      if ( uri.isEmpty() || uri.equals( Neo4jConstants.MISSING_NODE_NAME ) ) {
         return "";
      }
      return uri.replace( "_Stage_Finding", "" )
                    .replace( "_Stage", "" )
                    .replace( "_minus", "-" )
                    .replace( "_i", "i" );
   }

   protected void fillEvidenceMap( final AttributeInfoCollector infoCollector, final Map<String,String> dependencies) {
      useAllEvidenceMap( infoCollector, dependencies );
   }

   static private final List<String> SORT_LIST = Arrays.asList(
         "", "0i_p_", "0mol_p_", "0i_n_", "0mol_n_", "a",
         "x", "0i", "is", "0",
         "1", "1mi", "1mic", "1a", "1a1", "1a2", "1b", "1b1", "1b2", "1b3", "1b4", "1b4a", "1b4b", "1b4c", "1c",
         "2", "2a", "2b", "2c", "2d",
         "3", "3a", "3b", "3b1", "3b2", "3c", "3d",
         "4", "4a", "4a1", "4a2", "4b", "4c", "4d"
         );





//INSERT INTO DPHE_URI VALUES(2732596,'PT3a_b_Category')
//INSERT INTO DPHE_URI VALUES(441968,'Metastasis_Stage_M2')
//INSERT INTO DPHE_URI VALUES(730461,'Stage_1B1')
//INSERT INTO DPHE_URI VALUES(3869908,'Stage_4A2')
//INSERT INTO DPHE_URI VALUES(3869907,'Stage_4A1')






   //INSERT INTO DPHE_URI VALUES(332392,'PT2_Stage')
//INSERT INTO DPHE_URI VALUES(332391,'PT1_Stage')
//INSERT INTO DPHE_URI VALUES(332390,'PT0_Stage')
//INSERT INTO DPHE_URI VALUES(332399,'PN3_Stage')
//INSERT INTO DPHE_URI VALUES(332398,'PN2_Stage')
//INSERT INTO DPHE_URI VALUES(332397,'PN1_Stage')
//INSERT INTO DPHE_URI VALUES(332396,'PN0_Stage')
//INSERT INTO DPHE_URI VALUES(332395,'PTx_Stage')
//INSERT INTO DPHE_URI VALUES(332394,'PT4_Stage')
//INSERT INTO DPHE_URI VALUES(332393,'PT3_Stage')
//   "T5", "N4", "T5"
//
//INSERT INTO DPHE_URI VALUES(2733473,'PT1a2_Stage')
//   INSERT INTO DPHE_URI VALUES(2733467,'PT1a1_Stage')
//INSERT INTO DPHE_URI VALUES(3272364,'T1mi_Stage')
//INSERT INTO DPHE_URI VALUES(3272457,'N1mi_Stage')
//INSERT INTO DPHE_URI VALUES(3272458,'M0i_p__Stage')
//   INSERT INTO DPHE_URI VALUES(3272442,'N0mol_n__Stage')
//INSERT INTO DPHE_URI VALUES(3272441,'N0i_p__Stage')
//INSERT INTO DPHE_URI VALUES(3272443,'N0mol_p__Stage')
//INSERT INTO DPHE_URI VALUES(3272440,'N0i_n__Stage')
//   INSERT INTO DPHE_URI VALUES(3811917,'T1a2_Stage')
//INSERT INTO DPHE_URI VALUES(2733082,'PT1mic_Stage')

//   INSERT INTO DPHE_URI VALUES(2733109,'PN0i_p__Stage')
//   INSERT INTO DPHE_URI VALUES(2733254,'PT1b1_Stage')
//   INSERT INTO DPHE_URI VALUES(475751,'T4_Stage')
//   INSERT INTO DPHE_URI VALUES(441971,'M1_Stage')
//   INSERT INTO DPHE_URI VALUES(475390,'T3a_Stage')
//INSERT INTO DPHE_URI VALUES(475392,'T3b1_Stage')
//INSERT INTO DPHE_URI VALUES(475391,'T3b_Stage')
//INSERT INTO DPHE_URI VALUES(475398,'T4d_Stage')
//INSERT INTO DPHE_URI VALUES(475397,'T4c_Stage')
//INSERT INTO DPHE_URI VALUES(475394,'T3c_Stage')
//INSERT INTO DPHE_URI VALUES(475393,'T3b2_Stage')
//INSERT INTO DPHE_URI VALUES(475396,'T4b_Stage')
//INSERT INTO DPHE_URI VALUES(475395,'T4a_Stage')
//INSERT INTO DPHE_URI VALUES(475387,'T2a_Stage')
//INSERT INTO DPHE_URI VALUES(475386,'T1c_Stage')
//INSERT INTO DPHE_URI VALUES(475389,'T2c_Stage')
//INSERT INTO DPHE_URI VALUES(475388,'T2b_Stage')
//INSERT INTO DPHE_URI VALUES(475383,'T1a_Stage')
//INSERT INTO DPHE_URI VALUES(475385,'T1b_Stage')
   //INSERT INTO DPHE_URI VALUES(475372,'T1_Stage')
//INSERT INTO DPHE_URI VALUES(475371,'T0_Stage')
//INSERT INTO DPHE_URI VALUES(475374,'T3_Stage')
//INSERT INTO DPHE_URI VALUES(475373,'T2_Stage')
//INSERT INTO DPHE_URI VALUES(441959,'N0_Stage')
//INSERT INTO DPHE_URI VALUES(441960,'N2_Stage')
//INSERT INTO DPHE_URI VALUES(441962,'N1_Stage')
//INSERT INTO DPHE_URI VALUES(441961,'N3_Stage')
//INSERT INTO DPHE_URI VALUES(475413,'Tis_Stage')
//   INSERT INTO DPHE_URI VALUES(475400,'T1a1_Stage')
//INSERT INTO DPHE_URI VALUES(2732987,'PN0mol_p__Stage')
//   INSERT INTO DPHE_URI VALUES(730459,'T1b2_Stage')
//INSERT INTO DPHE_URI VALUES(730458,'T1b1_Stage')
//   INSERT INTO DPHE_URI VALUES(730456,'T1mic_Stage')
//INSERT INTO DPHE_URI VALUES(2732524,'PT3d_Stage')
//   INSERT INTO DPHE_URI VALUES(2732595,'PN0mol_n__Stage')
//INSERT INTO DPHE_URI VALUES(2732594,'PN0i_n__Stage')
//INSERT INTO DPHE_URI VALUES(2732831,'PN1mi_Stage')
//   INSERT INTO DPHE_URI VALUES(456908,'N1b_Stage')
//   INSERT INTO DPHE_URI VALUES(456906,'N1a_Stage')
//   INSERT INTO DPHE_URI VALUES(456987,'N1b4_Stage')
//   INSERT INTO DPHE_URI VALUES(456982,'N1b1_Stage')
//   INSERT INTO DPHE_URI VALUES(456985,'N1b2_Stage')
//INSERT INTO DPHE_URI VALUES(456986,'N1b3_Stage')
//   INSERT INTO DPHE_URI VALUES(456957,'PM1_Stage')
   //   INSERT INTO DPHE_URI VALUES(1276598,'Ta_Stage')
//   INSERT INTO DPHE_URI VALUES(1276680,'M1b4b_Stage')
//INSERT INTO DPHE_URI VALUES(1276681,'M1b4c_Stage')
//INSERT INTO DPHE_URI VALUES(1276678,'M1b4_Stage')
//INSERT INTO DPHE_URI VALUES(1276679,'M1b4a_Stage')
//INSERT INTO DPHE_URI VALUES(1711115,'PM1a_Stage')
//INSERT INTO DPHE_URI VALUES(1711116,'PM1b_Stage')
//INSERT INTO DPHE_URI VALUES(1711117,'PM1c_Stage')
//INSERT INTO DPHE_URI VALUES(1711118,'PN1a_Stage')
//   INSERT INTO DPHE_URI VALUES(1711119,'PN1b_Stage')
//   INSERT INTO DPHE_URI VALUES(1711130,'PT1c_Stage')
//INSERT INTO DPHE_URI VALUES(1711131,'PT2a_Stage')
//INSERT INTO DPHE_URI VALUES(1711132,'PT2b_Stage')
//INSERT INTO DPHE_URI VALUES(1711137,'PT4a_Stage')
//INSERT INTO DPHE_URI VALUES(1711138,'PT4b_Stage')
//INSERT INTO DPHE_URI VALUES(1711139,'PT4c_Stage')
//INSERT INTO DPHE_URI VALUES(1711133,'PT2c_Stage')
//INSERT INTO DPHE_URI VALUES(1711134,'PT3a_Stage')
//INSERT INTO DPHE_URI VALUES(1711135,'PT3b_Stage')
//INSERT INTO DPHE_URI VALUES(1711136,'PT3c_Stage')
//INSERT INTO DPHE_URI VALUES(1711120,'PN1c_Stage')
//INSERT INTO DPHE_URI VALUES(1711121,'PN2a_Stage')
//INSERT INTO DPHE_URI VALUES(1711126,'PN3c_Stage')
//INSERT INTO DPHE_URI VALUES(1711128,'PT1a_Stage')
//INSERT INTO DPHE_URI VALUES(1711129,'PT1b_Stage')
//INSERT INTO DPHE_URI VALUES(1711122,'PN2b_Stage')
//INSERT INTO DPHE_URI VALUES(1711123,'PN2c_Stage')
//INSERT INTO DPHE_URI VALUES(1711124,'PN3a_Stage')
//INSERT INTO DPHE_URI VALUES(1711125,'PN3b_Stage')
//INSERT INTO DPHE_URI VALUES(1711140,'PT4d_Stage')

//   INSERT INTO DPHE_URI VALUES(1300923,'PTa_Stage')
//   INSERT INTO DPHE_URI VALUES(445079,'N2a_Stage')
//   INSERT INTO DPHE_URI VALUES(445081,'N2c_Stage')
//INSERT INTO DPHE_URI VALUES(445080,'N2b_Stage')
//INSERT INTO DPHE_URI VALUES(445085,'Nx_Stage')
//   INSERT INTO DPHE_URI VALUES(445064,'M1b_Stage')
//INSERT INTO DPHE_URI VALUES(445036,'M1a_Stage')
//INSERT INTO DPHE_URI VALUES(445034,'M0_Stage')
//INSERT INTO DPHE_URI VALUES(445039,'Mx_Stage')
//INSERT INTO DPHE_URI VALUES(445037,'M1c_Stage')
//INSERT INTO DPHE_URI VALUES(1709110,'N3c_Stage')
//   INSERT INTO DPHE_URI VALUES(1709109,'N3b_Stage')
//   INSERT INTO DPHE_URI VALUES(1709108,'N3a_Stage')
//   INSERT INTO DPHE_URI VALUES(1709107,'N1c_Stage')
//   INSERT INTO DPHE_URI VALUES(3854131,'T3d_Stage')
//INSERT INTO DPHE_URI VALUES(3854130,'T2d_Stage')
//INSERT INTO DPHE_URI VALUES(332402,'PM0_Stage')
//INSERT INTO DPHE_URI VALUES(332401,'PNx_Stage')
//   INSERT INTO DPHE_URI VALUES(2732620,'PT1b2_Stage')
//   INSERT INTO DPHE_URI VALUES(332377,'Tx_Stage')
//   INSERT INTO DPHE_URI VALUES(332389,'PTis_Stage')


}
