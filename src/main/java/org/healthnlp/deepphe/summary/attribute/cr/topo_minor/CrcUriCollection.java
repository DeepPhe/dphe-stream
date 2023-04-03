package org.healthnlp.deepphe.summary.attribute.cr.topo_minor;

import org.healthnlp.deepphe.core.neo4j.Neo4jOntologyConceptUtil;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

/**
 * @author SPF , chip-nlp
 * @since {3/28/2023}
 */
public enum CrcUriCollection {
   INSTANCE;

   static public CrcUriCollection getInstance() {
      return INSTANCE;
   }

   private Collection<String> _allColonUris;
   private Collection<String> _cecumUris;
   private Collection<String> _appendixUris;
   private final String _ascendingUri = "Right_Colon";
   private final String _hepaticUri = "Hepatic_Flexure";
   private final Collection<String> _transverseUris
         = Arrays.asList( "Transverse_Colon", "Transverse_Mesocolon" );
   private final String _splenicUri = "Splenic_Flexure";
   private final Collection<String> _descendingUris
         = Arrays.asList( "Left_Colon", "Descending_Mesocolon" );
   private Collection<String> _sigmoidUris;
   private Collection<String> _colonUris;


   // C19.9
   private final String _rectosigmoidUri = "Rectosigmoid_Region";


   private final Collection<String> _anorectalUris
         = Arrays.asList( "Anorectal_Junction", "Anorectal_junction" );

   private Collection<String> _allAnusUris;
   private Collection<String> _analCanalUris;
   private Collection<String> _anusUris;
   private final String _anorectalUri = "Anorectal";
   private final String _cloacogenicZone = "Cloacogenic_Zone";

   private void initColonUris() {
      _cecumUris = Neo4jOntologyConceptUtil.getBranchUris( "Cecum" );
      _appendixUris = Neo4jOntologyConceptUtil.getBranchUris( "Appendix" );
      _colonUris = Neo4jOntologyConceptUtil.getBranchUris( "Colon" );
      _colonUris.remove( _rectosigmoidUri );

      _allColonUris = new HashSet<>();
      _allColonUris.addAll( _cecumUris );
      _allColonUris.addAll( _appendixUris );
      _allColonUris.addAll( _colonUris );
      _allColonUris.addAll( _anorectalUris );
      // Need laterality for ascending/descending determination,
      // but cannot use icdo laterality code as for C18 the code is always 9 (no laterality)
//      ALL_COLON_URIS.add( "Right" );
//      ALL_COLON_URIS.add( "Left" );
      _colonUris.remove( _ascendingUri );
      _colonUris.remove( _hepaticUri );
      _colonUris.removeAll( _transverseUris );
      _colonUris.removeAll( _descendingUris );
      _colonUris.remove( _splenicUri );
      _sigmoidUris = Neo4jOntologyConceptUtil.getBranchUris( "Sigmoid_Colon" );
      _sigmoidUris.remove( _rectosigmoidUri );
      _colonUris.removeAll( _sigmoidUris );
      initAnusUris();
   }

   private void initAnusUris() {
//      RECTAL_URIS = Neo4jOntologyConceptUtil.getBranchUris( "Rectal" );
//      // Rectosigmoid is C19.9, so we don't want it here.
//      RECTAL_URIS.remove( "Rectosigmoid_Colon" );
//      RECTAL_URIS.remove( RECTOSIGMOID_URI );
//      RECTAL_URIS.removeAll( ANORECTAL_URIS );
      _anusUris = Neo4jOntologyConceptUtil.getBranchUris( "Anus" );
      _anusUris.removeAll( _anorectalUris );

      _allAnusUris = new HashSet<>();
//      ALL_ANUS_URIS.addAll( RECTAL_URIS );
      _allAnusUris.addAll( _anusUris );

      _analCanalUris = Neo4jOntologyConceptUtil.getBranchUris( "Anal_Canal" );
      _anusUris.removeAll( _analCanalUris );
   }

   CrcUriCollection() {
      initColonUris();
   }

   public Collection<String> getAllColonUris() {
      return _allColonUris;
   }

   public Collection<String> getCecumUris() {
      return _cecumUris;
   }

   public Collection<String> getAppendixUris() {
      return _appendixUris;
   }

   public String getAscendingUri() {
      return _ascendingUri;
   }

   public String getHepaticUri() {
      return _hepaticUri;
   }

   public Collection<String> getTransverseUris() {
      return _transverseUris;
   }

   public String getSplenicUri() {
      return _splenicUri;
   }

   public Collection<String> getDescendingUris() {
      return _descendingUris;
   }

   public Collection<String> getSigmoidUris() {
      return _sigmoidUris;
   }

   public Collection<String> getColonUris() {
      return _colonUris;
   }

   public String getRectosigmoidUri() {
      return _rectosigmoidUri;
   }

   public Collection<String> getAnorectalUris() {
      return _anorectalUris;
   }

   public Collection<String> getAllAnusUris() {
      return _allAnusUris;
   }

   public Collection<String> getAnalCanalUris() {
      return _analCanalUris;
   }

   public Collection<String> getAnusUris() {
      return _anusUris;
   }

   public String getAnorectalUri() {
      return _anorectalUri;
   }

   public String getCloacogenicZone() {
      return _cloacogenicZone;
   }

}
