package org.healthnlp.deepphe;

import org.healthnlp.deepphe.core.neo4j.Neo4jOntologyConceptUtil;
import org.healthnlp.deepphe.neo4j.embedded.EmbeddedConnection;
import org.healthnlp.deepphe.neo4j.util.SearchUtil;
import org.neo4j.graphdb.GraphDatabaseService;

import java.util.Collection;

/**
 * @author SPF , chip-nlp
 * @since {3/5/2023}
 */
final public class BranchListerUtil {

   public static void main( String[] args ) {
      EmbeddedConnection.getInstance().connectToGraph();
      final GraphDatabaseService graphDb = EmbeddedConnection.getInstance().getGraph();

      final String ROOT_URI  = "Malignant_Tumor_Of_Colon";

      final Collection<String> rootUris = SearchUtil.getRootUris( graphDb, ROOT_URI );
      System.out.println( "Root Uris for " + ROOT_URI );
      for ( String uri : rootUris ) {
         int level = -10;
         try {
            level = Neo4jOntologyConceptUtil.getClassLevel( uri );
         } catch ( org.neo4j.graphdb.NotFoundException nfE ) {
            level = -100;
         }
         System.out.println( uri + " " + level );
      }

      System.out.println( "\n");

      final Collection<String> branchUris = SearchUtil.getBranchUris( graphDb, ROOT_URI );
      System.out.println( "Branch Uris for " + ROOT_URI );
      for ( String uri : branchUris ) {
         int level = -10;
         try {
            level = Neo4jOntologyConceptUtil.getClassLevel( uri );
         } catch ( org.neo4j.graphdb.NotFoundException nfE ) {
            level = -100;
         }
//         if ( level > 1 ) {
//            continue;
//         }
         System.out.println( uri + " " + level );
      }

   }





}
