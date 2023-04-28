package org.healthnlp.deepphe.summary.attribute.cr.topo_minor.ovary;

import org.healthnlp.deepphe.core.neo4j.Neo4jOntologyConceptUtil;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

/**
 * @author SPF , chip-nlp
 * @since {3/28/2023}
 */
public enum OvaryUriCollection {
   INSTANCE;

   static public OvaryUriCollection getInstance() {
      return INSTANCE;
   }

   // C48  1, 8
   private Collection<String> _allPeritoneumUris;
   private Collection<String> _peritoneumPartUris;
   private Collection<String> _overlappingRpUris;

   // C57
   private Collection<String> _allGenitalUris;
   private Collection<String> _fallopianTubeUris;
   private Collection<String> _broadLigamentUris;
   private Collection<String> _roundLigamentUris;
   private Collection<String> _parametriumUris;
   // Appendage_Of_The_Uterus   contains Fallopian Tube, Ligaments, Parametrium, Tract
   private Collection<String> _uterineAdnexaUris;
   private Collection<String> _otherGenitalUris;
   private Collection<String> _overlappingGenitalUris;
   private Collection<String> _genitalTractUris;  // Female_Genitourinary_Tract__NOS   Entire_Upper_Genitourinary_Tract


   // Peritoneum NOS = C48.2

   OvaryUriCollection() {
      initOvaryUris();
   }


   private void initOvaryUris() {
      _peritoneumPartUris = Neo4jOntologyConceptUtil.getBranchUris( "Peritoneum" );
      _allPeritoneumUris = new HashSet<>( _peritoneumPartUris );
      _peritoneumPartUris.remove( "Peritoneum" );
      _overlappingRpUris = Neo4jOntologyConceptUtil.getBranchUris( "Retroperitoneum" );
      _allPeritoneumUris.addAll( _overlappingRpUris );

      _allGenitalUris = Neo4jOntologyConceptUtil.getBranchUris( "Female_Genitalia" );
      _allGenitalUris.remove( "Placenta_Part" );
      _fallopianTubeUris = Neo4jOntologyConceptUtil.getBranchUris( "Fallopian_Tube" );
      _broadLigamentUris = Neo4jOntologyConceptUtil.getBranchUris( "Broad_Ligament" );
      _roundLigamentUris = Neo4jOntologyConceptUtil.getBranchUris( "Round_Ligament" );
      _allGenitalUris.addAll( _roundLigamentUris );
      _parametriumUris = Neo4jOntologyConceptUtil.getBranchUris( "Parametrium" );
      _overlappingGenitalUris = Neo4jOntologyConceptUtil.getBranchUris( "Entire_Tubal_End_Of_Ovary" );
      _uterineAdnexaUris = Neo4jOntologyConceptUtil.getBranchUris( "Appendage_Of_The_Uterus" );
      _uterineAdnexaUris.removeAll( _fallopianTubeUris );
      _uterineAdnexaUris.removeAll( _broadLigamentUris );
      _uterineAdnexaUris.removeAll( _roundLigamentUris );
      _uterineAdnexaUris.removeAll( _parametriumUris );
      _uterineAdnexaUris.removeAll( _overlappingGenitalUris );
      _uterineAdnexaUris.remove( "Placenta_Part" );
      _uterineAdnexaUris.remove( "Female_Genitourinary_Tract__NOS" );
      _otherGenitalUris = Neo4jOntologyConceptUtil.getBranchUris( "Female_Genitalia" );
      _otherGenitalUris.removeAll( _fallopianTubeUris );
      _otherGenitalUris.removeAll( _broadLigamentUris );
      _otherGenitalUris.removeAll( _roundLigamentUris );
      _otherGenitalUris.removeAll( _parametriumUris );
      _otherGenitalUris.removeAll( _overlappingGenitalUris );
      _otherGenitalUris.remove( "Placenta_Part" );
      _otherGenitalUris.remove( "Female_Genitourinary_Tract__NOS" );
      // Tract NOS contains a ton of stuff, including male.  Just use the exact uri.
      _genitalTractUris = Collections.singletonList( "Female_Genitourinary_Tract__NOS" );
   }


   Collection<String> getAllPeritoneumUris() {
      return _allPeritoneumUris;
   }

   Collection<String> getPeritoneumPartUris() {
      return _peritoneumPartUris;
   }

   Collection<String> getOverlappingRpUris() {
      return _overlappingRpUris;
   }


   Collection<String> getAllGenitalUris() {
      return _allGenitalUris;
   }

   Collection<String> getFallopianTubeUris() {
      return _fallopianTubeUris;
   }

   Collection<String> getBroadLigamentUris() {
      return _broadLigamentUris;
   }

   Collection<String> getRoundLigamentUris() {
      return _roundLigamentUris;
   }

   Collection<String> getParametriumUris() {
      return _parametriumUris;
   }

   Collection<String> getUterineAdnexaUris() {
      return _uterineAdnexaUris;
   }

   Collection<String> getOtherGenitalUris() {
      return _otherGenitalUris;
   }

   Collection<String> getOverlappingGenitalUris() {
      return _overlappingGenitalUris;
   }

   Collection<String> getGenitalTractUris() {
      return _genitalTractUris;
   }


}
