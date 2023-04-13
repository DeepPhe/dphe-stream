package org.healthnlp.deepphe.summary.attribute.cr.topo_minor.lung;

import org.healthnlp.deepphe.core.neo4j.Neo4jOntologyConceptUtil;

import java.util.Collection;
import java.util.HashSet;

/**
 * @author SPF , chip-nlp
 * @since {3/28/2023}
 */
public enum LungUriCollection {
   INSTANCE;

   static public LungUriCollection getInstance() {
      return INSTANCE;
   }

   private Collection<String> _lungUris;
   private Collection<String> _bronchusUris;
   private Collection<String> _upperLobeUris;
   private Collection<String> _middleLobeUris;
   private Collection<String> _lowerLobeUris;
   private Collection<String> _tracheaUris;

   LungUriCollection() {
      initLungUris();
   }

   private void initLungUris() {
      if ( _lungUris != null ) {
         return;
      }
      _lungUris = Neo4jOntologyConceptUtil.getBranchUris( "Lung" );
      _bronchusUris = Neo4jOntologyConceptUtil.getBranchUris( "Bronchus" );
      _upperLobeUris = new HashSet<>( Neo4jOntologyConceptUtil.getBranchUris( "Upper_Lobe_Of_The_Lung" ) );
      _upperLobeUris.addAll( Neo4jOntologyConceptUtil.getBranchUris( "Upper_Zone_Of_Lung" ) );
      _middleLobeUris = new HashSet<>( Neo4jOntologyConceptUtil.getBranchUris( "Middle_Lobe_Of_The_Right_Lung" ) );
      _middleLobeUris.addAll( Neo4jOntologyConceptUtil.getBranchUris( "Middle_Zone_Of_Right_Lung" ) );
      _lowerLobeUris = new HashSet<>( Neo4jOntologyConceptUtil.getBranchUris( "Lower_Lung_Lobe" ) );  //  Zone
      _lowerLobeUris.addAll( Neo4jOntologyConceptUtil.getBranchUris( "Lower_Zone_Of_Lung" ) );
      _tracheaUris = Neo4jOntologyConceptUtil.getBranchUris( "Trachea" );
   }


   public Collection<String> getLungUris() {
      return _lungUris;
   }

   public Collection<String> getBronchusUris() {
      return _bronchusUris;
   }

   public Collection<String> getUpperLobeUris() {
      return _upperLobeUris;
   }

   public Collection<String> getMiddleLobeUris() {
      return _middleLobeUris;
   }

   public Collection<String> getLowerLobeUris() {
      return _lowerLobeUris;
   }

   public Collection<String> getTracheaUris() {
      return _tracheaUris;
   }

}
