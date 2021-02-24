package org.healthnlp.deepphe.summary.engine;


import org.apache.log4j.Logger;
import org.healthnlp.deepphe.neo4j.constant.UriConstants;
import org.healthnlp.deepphe.neo4j.embedded.EmbeddedConnection;
import org.healthnlp.deepphe.neo4j.node.NeoplasmAttribute;
import org.healthnlp.deepphe.neo4j.node.NeoplasmSummary;
import org.healthnlp.deepphe.summary.attribute.grade.Grade;
import org.healthnlp.deepphe.summary.attribute.laterality.Laterality;
import org.healthnlp.deepphe.summary.attribute.morphology.Morphology;
import org.healthnlp.deepphe.summary.attribute.topography.Topography;
import org.healthnlp.deepphe.summary.concept.ConceptAggregate;
import org.healthnlp.deepphe.util.TopoMorphValidator;
import org.neo4j.graphdb.GraphDatabaseService;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.healthnlp.deepphe.neo4j.constant.RelationConstants.*;



// Output files from System : 1 file for each attribute.  Comma separated.
//   attribute_name.csv
//       neoplasm_id,attribute_name,value,8,8,10,10,10
//       neoplasm_id,attribute_name,value,7,6,10,5,10
//       neoplasm_id,attribute_name,value,1,1,7,8,9
//       bobs_cancer,topo_major,C50,1,1,7,8,9

// Fed to Eval script.  New Output file from Eval:
//   attribute_name_eval.csv
//       neoplasm_id,attribute_name,value,8,8,10,10,10  , F1
//       neoplasm_id,attribute_name,value,7,6,10,5,10  , F1
//       neoplasm_id,attribute_name,value,1,1,7,8,9   , F1
//       bobs_cancer,topo_major,C50,1,1,7,8,9  , (c42)  ->  (0)  ->   review


/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 7/15/2020
 */
final public class NeoplasmSummaryCreator {

   static private final Logger LOGGER = Logger.getLogger( "NeoplasmSummaryCreator" );


   private NeoplasmSummaryCreator() {}



   static public NeoplasmSummary createNeoplasmSummary( final ConceptAggregate neoplasm,
                                                        final Collection<ConceptAggregate> allConcepts ) {
      final GraphDatabaseService graphDb = EmbeddedConnection.getInstance()
                                                             .getGraph();
      final Collection<String> massNeoplasmUris = UriConstants.getMassNeoplasmUris( graphDb );
      final Predicate<ConceptAggregate> isNeoplasm = c -> c.getAllUris()
                                                           .stream()
                                                           .anyMatch( massNeoplasmUris::contains );
      final Collection<ConceptAggregate> patientNeoplasms
            = allConcepts.stream()
                         .filter( isNeoplasm )
                         .collect( Collectors.toList() );

      final NeoplasmSummary summary = new NeoplasmSummary();
      summary.setId( neoplasm.getUri() );
      final List<NeoplasmAttribute> attributes = new ArrayList<>();

      final String topoCode = addTopography( neoplasm, summary, attributes, allConcepts );
      addMorphology( neoplasm, summary, attributes, allConcepts, patientNeoplasms, topoCode );
      addLaterality( neoplasm, summary, attributes, allConcepts, patientNeoplasms );
      addGrade( neoplasm, summary, attributes, allConcepts );

      summary.setPathologic_t( getT( neoplasm ) );
      summary.setPathologic_n( getN( neoplasm ) );
      summary.setPathologic_m( getM( neoplasm ) );
      summary.setEr( getEr( neoplasm ) );
      summary.setPr( getPr( neoplasm ) );
      summary.setHer2( getHer2( neoplasm ) );
      summary.setKi67( getKi67( neoplasm ) );
      summary.setPsa( getPsa( neoplasm ) );

      summary.setAttributes( attributes );
      return summary;
   }

   static private String addTopography( final ConceptAggregate neoplasm,
                                      final NeoplasmSummary summary,
                                      final List<NeoplasmAttribute> attributes,
                                        final Collection<ConceptAggregate> allConcepts ) {
      final Topography topography = new Topography( neoplasm, allConcepts );
      final NeoplasmAttribute majorTopoAttr = topography.toNeoplasmAttribute();
      attributes.add( majorTopoAttr );
      final NeoplasmAttribute minorTopoAttr = topography.getMinorTopography();
      attributes.add( minorTopoAttr );

      summary.setSite_major( topography.getMajorSiteUri() );
      summary.setSite_minor( topography.getMinorSiteUri() );
      summary.setTopography_major( topography.getBestMajorTopoCode() );
      summary.setTopography_minor( topography.getBestMinorTopoCode() );

      // TODO as NeoplasmAttribute
      summary.setSite_related( getSiteRelated( neoplasm ) );

      return majorTopoAttr.getValue() + minorTopoAttr.getValue();
   }

   static private void addMorphology( final ConceptAggregate neoplasm,
                                        final NeoplasmSummary summary,
                                        final List<NeoplasmAttribute> attributes,
                                        final Collection<ConceptAggregate> allConcepts,
                                      final Collection<ConceptAggregate> patientNeoplasms,
                                        final String topographyCode ) {
      final Collection<String> validTopoMorphs = TopoMorphValidator.getInstance()
                                                                   .getValidTopoMorphs( topographyCode );
      final Morphology morphology = new Morphology( neoplasm, allConcepts, patientNeoplasms, validTopoMorphs, topographyCode );
      final NeoplasmAttribute histologyAttr = morphology.toNeoplasmAttribute();
      attributes.add( histologyAttr );
      final NeoplasmAttribute behaviorAttr = morphology.getBehaviorAttribute();
      attributes.add( behaviorAttr );

      summary.setHistology( morphology.getBestHistoCode() );
      summary.setBehavior( morphology.getBestBehaveCode() );
   }


   static private String getSiteRelated( final ConceptAggregate conceptAggregate ) {
      return String.join( ";", getRelatedUris( conceptAggregate, HAS_QUADRANT, HAS_CLOCKFACE ) );
   }



   static private void addLaterality( final ConceptAggregate neoplasm,
                                 final NeoplasmSummary summary,
                                 final List<NeoplasmAttribute> attributes,
                                 final Collection<ConceptAggregate> allConcepts,
                                      final Collection<ConceptAggregate> patientNeoplasms ) {
      final Laterality laterality = new Laterality( neoplasm, allConcepts, patientNeoplasms );
      final NeoplasmAttribute lateralityAttribute = laterality.toNeoplasmAttribute();
      attributes.add( lateralityAttribute );
      summary.setLaterality( laterality.getBestLaterality() );
      summary.setLaterality_code( laterality.getBestLateralityCode() );
   }


   static private void addGrade( final ConceptAggregate neoplasm,
                                        final NeoplasmSummary summary,
                                        final List<NeoplasmAttribute> attributes,
                                        final Collection<ConceptAggregate> allConcepts ) {
      final Grade grade = new Grade( neoplasm, allConcepts );
      final NeoplasmAttribute gradeAttribute = grade.toNeoplasmAttribute();
      attributes.add( gradeAttribute );
      summary.setGrade( grade.getBestGradeCode() );
   }




   static private String getT( final ConceptAggregate summary ) {
      final Collection<String> ts = new HashSet<>();
      ts.addAll( getRelatedUris( summary, HAS_CLINICAL_T ) );
      ts.addAll( getRelatedUris( summary, HAS_PATHOLOGIC_T ) );
      return String.join( ";", getTnmValue( ts, 't', true ) );
   }

   static private String getN( final ConceptAggregate summary ) {
      final Collection<String> ns = new HashSet<>();
      ns.addAll( getRelatedUris( summary, HAS_CLINICAL_N ) );
      ns.addAll( getRelatedUris( summary, HAS_PATHOLOGIC_N ) );
      return String.join( ";", getTnmValue( ns, 'n', true ) );
   }

   static private String getM( final ConceptAggregate summary ) {
      final Collection<String> ms = new HashSet<>();
      ms.addAll( getRelatedUris( summary, HAS_CLINICAL_M ) );
      ms.addAll( getRelatedUris( summary, HAS_PATHOLOGIC_M ) );
      return String.join( ";", getTnmValue( ms, 'm', false ) );
   }

   static private String getTnmValue( final Collection<String> tnms, final char type, final boolean allowX ) {
      final Collection<String> values = new HashSet<>();
      for ( String tnm : tnms ) {
         final String lower = tnm.toLowerCase().replace( "_stage", "" );
         final int typeIndex = lower.indexOf( type );
         if ( typeIndex < 0 || typeIndex >= tnm.length() - 1 ) {
            continue;
         }
         final String value = lower.substring( typeIndex + 1 );
         values.add( value );
      }
      values.remove( "x_category" );
      if ( !allowX ) {
         values.remove( "x" );
         values.remove( "X" );
      }
      return String.join( ";", values );
   }


   static private String getEr( final ConceptAggregate summary ) {
      return getErPrHer2( summary, HAS_ER_STATUS );
   }

   static private String getPr( final ConceptAggregate summary ) {
      return getErPrHer2( summary, HAS_PR_STATUS );
   }

   static private String getHer2( final ConceptAggregate summary ) {
      return getErPrHer2( summary, HAS_HER2_STATUS );
   }

   static private String getErPrHer2( final ConceptAggregate summary, final String relation ) {
      final Collection<String> uris = getRelatedUris( summary, relation );
      if ( uris.isEmpty() ) {
         return "";
      }
      final Collection<String> statValues = Arrays.asList( "Positive", "Negative", "Equivocal", "Indeterminate" );
      final Collection<String> values = new HashSet<>();
      for ( String uri : uris ) {
         for ( String status : statValues ) {
            if ( uri.endsWith( status ) ) {
               values.add( status );
               break;
            }
         }
      }
      return String.join( ";", values );
   }

   static private String getKi67( final ConceptAggregate summary ) {
//      final Collection<String> ki67s = getRelatedTexts( summary, HAS_KI67_SCORE );
//      return ki67s.stream()
//                  .map( NeoplasmSummaryWriter::getPercent )
//                  .filter( p -> !p.isEmpty() )
//                  .collect( Collectors.joining( ";" ) );
      return "";
   }

   static private String getPercent( final String ki67score ) {
      final int percentIndex = ki67score.indexOf( '%' );
      if ( percentIndex < 1 ) {
         return "";
      }
//      String percent = ki67score.substring( Math.max( 0, percentIndex-6 ), percentIndex+1 );
      String percent = ki67score.substring( 0, percentIndex );
      percent = percent.replace( "(", "" );
      percent = percent.replace( "[", "" );
//      percent = percent.replace( "%", "" );
      percent = percent.replace( " ", "" );
      return percent.trim();
   }

   static private String getPsa( final ConceptAggregate summary ) {
//      final Collection<String> psas = getRelatedTexts( summary, HAS_PSA_LEVEL );
//      final Collection<String> elevateds = psas.stream()
//                                               .filter( p -> p.toLowerCase().contains( "elevated" ) )
//                                               .collect( Collectors.toList() );
//      if ( !elevateds.isEmpty() ) {
//         psas.removeAll( elevateds );
//         psas.add( "Elevated PSA" );
//      }
//      if ( psas.isEmpty() ) {
//         return "";
//      }
//      return psas.stream()
//                 .map( String::trim )
//                 .filter( p -> !p.isEmpty() )
//                 .collect( Collectors.joining( ";" ) );
      return "";
   }












   static private Collection<String> getRelatedUris( final ConceptAggregate conceptAggregate, final String... relations ) {
      return conceptAggregate.getRelatedUris( relations );
//      final Collection<String> relateds = new HashSet<>();
//      for ( String relation : relations ) {
//         final Collection<String> relatedUris = conceptAggregate.getRelatedUris( relation );
//         if ( relatedUris != null ) {
//            relateds.addAll( relatedUris );
//         }
//      }
//      return relateds;
   }




}
