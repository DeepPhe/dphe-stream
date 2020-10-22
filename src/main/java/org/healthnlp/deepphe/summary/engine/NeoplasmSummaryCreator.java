package org.healthnlp.deepphe.summary.engine;


import org.apache.log4j.Logger;
import org.healthnlp.deepphe.core.neo4j.Neo4jOntologyConceptUtil;
import org.healthnlp.deepphe.core.uri.UriUtil;
import org.healthnlp.deepphe.neo4j.node.NeoplasmSummary;
import org.healthnlp.deepphe.summary.concept.ConceptAggregate;
import org.healthnlp.deepphe.util.UriScoreUtil;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.healthnlp.deepphe.neo4j.constant.RelationConstants.*;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 7/15/2020
 */
final public class NeoplasmSummaryCreator {

   static private final Logger LOGGER = Logger.getLogger( "NeoplasmSummaryCreator" );


   private NeoplasmSummaryCreator() {}



   static public NeoplasmSummary createNeoplasmSummary( final ConceptAggregate neoplasm ) {
      final NeoplasmSummary summary = new NeoplasmSummary();
      summary.setId( neoplasm.getUri() );
      final Collection<String> sites = getSites( neoplasm );
      summary.setSite_major( getMajorSite( sites ) );
      summary.setSite_minor( getMinorSite( sites ) );
      summary.setTopography_major( getMajorTopo( sites ) );
      summary.setTopography_minor( getMinorTopo( sites ) );
      summary.setSite_related( getSiteRelated( neoplasm ) );
      final Collection<String> morphs = getMorphology( neoplasm );
      summary.setHistology( getBestHistology( morphs ) );
      summary.setBehavior( getBestBehavior( morphs ) );
      final Collection<String> sides = getLateralities( neoplasm );
      summary.setLaterality( getLaterality( sides ) );
      summary.setLaterality_code( getLateralityIcdo( sides ) );
      summary.setGrade( getGrade( neoplasm ) );
      summary.setPathologic_t( getT( neoplasm ) );
      summary.setPathologic_n( getN( neoplasm ) );
      summary.setPathologic_m( getM( neoplasm ) );
      summary.setEr( getEr( neoplasm ) );
      summary.setPr( getPr( neoplasm ) );
      summary.setHer2( getHer2( neoplasm ) );
      summary.setKi67( getKi67( neoplasm ) );
      summary.setPsa( getPsa( neoplasm ) );
      return summary;
   }


   static private Collection<String> getSites( final ConceptAggregate neoplasm ) {
      return getFirstRelatedUris( neoplasm,
            DISEASE_HAS_PRIMARY_ANATOMIC_SITE,
            DISEASE_HAS_ASSOCIATED_ANATOMIC_SITE,
            DISEASE_HAS_METASTATIC_ANATOMIC_SITE,
            Disease_Has_Associated_Region,
            Disease_Has_Associated_Cavity );
   }

   static private String getMajorSite( final Collection<String> sites ) {
      LOGGER.info( "Major site according to shortest root uris for sites " + String.join( ",",sites ) );
      return UriScoreUtil.getBestUri( sites );
//      return UriUtil.getShortestRootUri( sites );
   }

   static private String getMinorSite( final Collection<String> sites ) {
      LOGGER.info( "Minor site according to most specific uri for sites " + String.join( ",",sites ) );
      return UriUtil.getMostSpecificUri( sites );
   }

   static private String getMajorTopo( final Collection<String> sites ) {
      LOGGER.info( "Major topography according to ICDO codes for sites " + String.join( ",", sites ) );
      if ( sites.isEmpty() ) {
         LOGGER.info( "No sites, using C80." );
         return "C80";
      }
      final Function<String, String> getMajor = t -> {
         final int dot = t.indexOf( '.' );
         return dot > 0 ? t.substring( 0, dot ) : t;
      };
      final Collection<String> topos = sites.stream()
                                           .map( Neo4jOntologyConceptUtil::getIcdoTopoCode )
                                           .collect( Collectors.toSet() );
      return topos.stream()
                  .map( getMajor )
                  .distinct()
                  .filter( t -> !t.isEmpty() )
                  .sorted()
                  .collect( Collectors.joining( ";" ) );
   }

   static private String getMinorTopo( final Collection<String> sites ) {
      LOGGER.info( "Major topography according to ICDO codes for sites " + String.join( ",", sites ) );
      if ( sites.isEmpty() ) {
         return "9";
      }
      final Function<String, String> getMinor = t -> {
         final int dot = t.indexOf( '.' );
         return dot > 0 ? t.substring( dot + 1 ) : "";
      };
      final Collection<String> topos = sites.stream()
                                           .map( Neo4jOntologyConceptUtil::getIcdoTopoCode )
                                           .collect( Collectors.toSet() );
      final Collection<String> allMinors = topos.stream()
                  .map( getMinor )
                  .distinct()
                  .filter( t -> !t.isEmpty() )
                  .sorted()
                  .collect( Collectors.toList() );
      if ( allMinors.size() > 1 ) {
         allMinors.remove( "9" );
      }
      String minors = String.join( ";", allMinors );
      if ( minors.isEmpty() ) {
         LOGGER.info( "No specific site codes, using 9." );
         return "9";
      }
      return minors;
   }

   static private String getSiteRelated( final ConceptAggregate conceptAggregate ) {
      return String.join( ";", getRelatedUris( conceptAggregate, HAS_QUADRANT, HAS_CLOCKFACE ) );
   }


//   static private Collection<String> getMorphology( final ConceptAggregate conceptAggregate ) {
////      final Collection<String> uris
////            = instance.getAnnotations().stream()
////                      .map( Neo4jOntologyConceptUtil::getUri )
////                      .filter( Objects::nonNull )
////                      .filter( u -> !u.isEmpty() )
////                      .collect( Collectors.toSet() );
////      if ( uris.isEmpty() ) {
////         return Collections.emptyList();
////      }
////      Collection<String> morphs = uris.stream()
////                                      .map( NeoplasmSummaryWriter::getIcdoMorphCodes )
////                                      .flatMap( Collection::stream )
////                                      .collect( Collectors.toSet() );
//      LOGGER.info( "Morphology seems to have a little bit of human favoratism involved ..." );
//
//      final String uri = conceptAggregate.getUri();
//      final Collection<String> morphs = new HashSet<>( getIcdoMorphCodes( uri ) );
//      LOGGER.info( "All Ontology Morphology codes for " + uri + ": " + String.join( ",", morphs ) );
//
////      if ( uris.contains( "Invasive_Breast_Carcinoma" ) && uris.contains( "Ductal_Carcinoma" ) ) {
//      if ( uri.equals( "Ductal_Carcinoma" ) ) {
//         // Kludge for invasive ductal
//         LOGGER.info( "Hardcoded Ductal_Carcinoma = 8500/3" );
//         morphs.add( "8500/3" );
//      }
//      if ( uri.equals( "Lung_Carcinoma" ) ) {
//         // Kludge for lung carcinoma  -- 8046/3 is non-small cell carcinoma.
//         LOGGER.info( "Hardcoded Lung_Carcinoma = 8046/3" );
//         morphs.add( "8046/3" );
//      }
//      return morphs;
////      final Collection<String> prefTexts = uris.stream()
////                                               .map( Neo4jOntologyConceptUtil::getPreferredText )
////                                               .collect( Collectors.toSet() );
////      return getBestHisto( morphs ) + B
////             + String.join( ";", prefTexts ) + ":" + String.join( ";", morphs ) + B
////             + getBestBehave( morphs ) + B
////             + String.join( ";", getRelatedUris( summary, HAS_TUMOR_EXTENT ) );
//   }


   static private Collection<String> getMorphology( final ConceptAggregate conceptAggregate ) {
      LOGGER.info( "Morphology seems to have a little bit of human favoritism involved ..." );

      final Collection<String> morphs
            = conceptAggregate.getAllUris()
                              .stream()
                              .map( NeoplasmSummaryCreator::getIcdoMorphCodes )
                              .flatMap( Collection::stream )
                              .collect( Collectors.toSet() );
      LOGGER.info( "All Ontology Morphology codes for " + conceptAggregate.getUri() + ": " + String.join( ",", morphs ) );

//      if ( uris.contains( "Invasive_Breast_Carcinoma" ) && uris.contains( "Ductal_Carcinoma" ) ) {
//      if ( uri.equals( "Ductal_Carcinoma" ) ) {
//         // Kludge for invasive ductal
//         LOGGER.info( "Hardcoded Ductal_Carcinoma = 8500/3" );
//         morphs.add( "8500/3" );
//      }
//      if ( uri.equals( "Lung_Carcinoma" ) ) {
//         // Kludge for lung carcinoma  -- 8046/3 is non-small cell carcinoma.
//         LOGGER.info( "Hardcoded Lung_Carcinoma = 8046/3" );
//         morphs.add( "8046/3" );
//      }
      return morphs;
   }






   static private Collection<String> getIcdoMorphCodes( final String uri ) {
      return Neo4jOntologyConceptUtil.getIcdoCodes( uri ).stream()
                                     .filter( i -> !i.startsWith( "C" ) )
                                     .filter( i -> !i.contains( "-" ) )
                                     .filter( i -> i.length() > 3 )
                                     .sorted()
                                     .collect( Collectors.toList() );
   }


   static private final Function<String, String> getHisto
         = m -> {
      final int i = m.indexOf( "/" );
      return i > 0 ? m.substring( 0, i ) : m;
   };
   static private final Function<String, String> getBehave
         = m -> {
      final int i = m.indexOf( "/" );
      return i > 0 ? m.substring( i + 1 ) : "";
   };



   static private String getBestHistology( final Collection<String> morphs ) {
      LOGGER.info( "Getting Best Histology from Morphology codes " + String.join( ",", morphs ) );
      final HistoComparator comparator = new HistoComparator();

      LOGGER.info( "The preferred histology is the first of the following OR the first in numerically sorted order:" );
      LOGGER.info( "8071 8070 8520 8575 8500 8503 8260 8250 8140 8480 8046 8041 8240 8012 8000 8010" );
      LOGGER.info( "This ordering came from the best overall fit to gold annotations." );

      return morphs.stream()
                   .map( getHisto )
                   .filter( h -> !h.isEmpty() )
//                   .max( String.CASE_INSENSITIVE_ORDER )
                   .max( comparator )
                   .orElse( "" );
   }


   // Should be 3 (instead of 2) : 2
   static private String getBestBehavior( final Collection<String> morphs ) {
      LOGGER.info( "Behavior comes from Histology." );
      final String histo = getBestHistology( morphs );
      if ( histo.isEmpty() ) {
         return "";
      }
      final List<String> behaves = morphs.stream()
                                         .filter( m -> m.startsWith( histo ) )
                                         .map( getBehave )
                                         .filter( b -> !b.isEmpty() )
                                         .distinct()
                                         .sorted()
                                         .collect( Collectors.toList() );
      if ( behaves.isEmpty() ) {
         return "";
      }
      if ( behaves.size() == 1 ) {
         LOGGER.info( "Only one possible behavior." );
         return behaves.get( 0 );
      }
      if ( behaves.size() == 2 && behaves.contains( "2" ) && behaves.contains( "3" ) ) {
         LOGGER.info( "Only Behaviors 2 and 3, and Behavior of 3 trumps a behavior of 2." );
         return "3";
      }
      LOGGER.info( "Removing Behavior 3 (if present) in favor of other highest value." );
      behaves.remove( "3" );
      return behaves.get( behaves.size() - 1 );
   }


   static private Collection<String> getLateralities( final ConceptAggregate conceptAggregate ) {
      return getRelatedUris( conceptAggregate, HAS_LATERALITY );
   }

   static private String getLaterality( final Collection<String> sides ) {
      return String.join( ";", sides );
   }

   static private String getLateralityIcdo( final Collection<String> sides ) {
      if ( sides.isEmpty() ) {
         return "";
      }
      final Collection<String> icdo = new HashSet<>();
      if ( sides.contains( "Right" ) ) {
         icdo.add( "1" );
      }
      if ( sides.contains( "Left" ) ) {
         icdo.add( "2" );
      }
      if ( sides.contains( "Bilateral" ) ) {
         icdo.add( "4" );
      }
      return String.join( ";", icdo );
   }


//   1   Grade 1
//   2   Grade 2
//   3   Grade 3
//   4   Grade 4
//   5   T-Cell
//   6   B-Cell
//   7   Null Cell
//   8   Natural Killer Cell
//   9   Unknown
   //    https://seer.cancer.gov/tools/grade/
   //    https://apps.who.int/iris/bitstream/handle/10665/96612/9789241548496_eng.pdf
   static private String getGrade( final ConceptAggregate summary ) {
      LOGGER.info( "Getting Grade ..." );
      // TODO Remove from ontology (class or synonyms)
//      gleasons.remove( "Gleason_Grade" );
//      gleasons.remove( "Total_Gleason_Score" );
      // TODO a bunch of grades end up being tied as findings and not grades.
      //  Fix in Ontology - or is it a section issue?
      // TODO add Nottingham lookup for BrCa, Bloom-Richardson for BrCa.
      final Collection<String> grades
            = getRelatedUris( summary, HAS_GLEASON_SCORE, DISEASE_IS_GRADE, DISEASE_HAS_FINDING );
      LOGGER.info( "Grade is derived from the related HAS_GLEASON_SCORE, DISEASE_IS_GRADE, DISEASE_HAS_FINDING" );
      final Collection<String> icdos = new HashSet<>();
      final Collection<String> goodGrades = new HashSet<>();
      for ( String grade : grades ) {
         if ( grade.startsWith( "Gleason_Score_" ) ) {
            LOGGER.info( "Have a Gleason Score, adding its Grade Equivalent to possible ICDO Grades." );
            if ( grade.endsWith( "7" ) ) {
               icdos.add( "2" );
            } else if ( grade.endsWith( "8" )
                        || grade.endsWith( "9" )
                        || grade.endsWith( "10" ) ) {
               icdos.add( "3" );
            } else if ( grade.endsWith( "6" ) ) {
               icdos.add( "1" );
            } else {
               continue;
            }
            goodGrades.add( grade );
         } else if ( grade.equals( "Grade_1" )
                     || grade.equals( "Low_Grade" )
                     || grade.equals( "Low_Grade_Malignant_Neoplasm" )
                     || grade.equals( "Well_Differentiated" ) ) {
            icdos.add( "1" );
            LOGGER.info( "Have a Low_Grade or Well_differentiated, adding its Grade Equivalent (1) to possible ICDO Grades." );
            goodGrades.add( grade );
         } else if ( grade.equals( "Grade_2" )
                     || grade.equals( "Intermediate_Grade" )
                     || grade.equals( "Intermediate_Grade_Malignant_Neoplasm" )
                     || grade.equals( "Moderately_Differentiated" ) ) {
            LOGGER.info( "Have a Intermediate_Grade or Moderately_Differentiated, adding its Grade Equivalent (2) to possible ICDO Grades." );
            icdos.add( "2" );
            goodGrades.add( grade );
         } else if ( grade.equals( "Grade_3" )
                     || grade.equals( "High_Grade" )
                     || grade.equals( "High_Grade_Malignant_Neoplasm" )
                     || grade.equals( "Poorly_Differentiated" ) ) {
            LOGGER.info( "Have a High_Grade or Poorly_Differentiated, adding its Grade Equivalent (3) to possible ICDO Grades." );
            icdos.add( "3" );
            goodGrades.add( grade );
         } else if ( grade.equals( "Grade_4" )
                     || grade.equals( "Undifferentiated" ) ) {
            // todo add "anaplastic"
            LOGGER.info( "Have an Undifferentiated, adding its Grade Equivalent (4) to possible ICDO Grades." );
            icdos.add( "4" );
            goodGrades.add( grade );
         } else if ( grade.equals( "Grade_5" ) ) {
            icdos.add( "5" );
            goodGrades.add( grade );
         }
      }
      if ( goodGrades.isEmpty() ) {
         LOGGER.info( "No Grades." );
         return "9";
      }
      LOGGER.info( "Highest grade (or 9) of " + String.join( ",", icdos ) );
      return icdos.stream().max( String.CASE_INSENSITIVE_ORDER ).orElse( "9" );
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
      final Collection<String> relateds = new HashSet<>();
      for ( String relation : relations ) {
         final Collection<String> relatedUris = conceptAggregate.getRelatedUris( relation );
         if ( relatedUris != null ) {
            relateds.addAll( relatedUris );
         }
      }
      return relateds;
   }


   static private Collection<String> getFirstRelatedUris( final ConceptAggregate conceptAggregate,
                                                          final String... relations ) {
      LOGGER.info( "Choosing best site for neoplasm based upon relation hierarchy." );
      for ( String relation : relations ) {
         LOGGER.info( "   " + relation );
         final Collection<String> relatedUris = conceptAggregate.getRelatedUris( relation );
         if ( relatedUris != null && !relatedUris.isEmpty() ) {
            LOGGER.info( "      " + String.join( ",", relatedUris ) );
            return relatedUris;
         }
      }
      return Collections.emptyList();
   }

//   static private Collection<String> getRelatedTexts( final ConceptAggregate summary, final String... relations ) {
//      final Collection<String> relateds = new HashSet<>();
//      for ( String relation : relations ) {
//         final Collection<ConceptAggregate> related = summary.getRelations().get( relation );
//         if ( related != null && !related.isEmpty() ) {
//            related.stream()
//                   .map( ConceptAggregate::getMentions )
//                   .flatMap( Collection::stream )
//                   .map( Mention::getCoveredText )
//                   .forEach( relateds::add );
//         }
//      }
//      return relateds;
//   }





      // https://apps.who.int/iris/bitstream/handle/10665/96612/9789241548496_eng.pdf
      // Histo M     8070,8046 8046,8140 8000,8010 8002,8041 8046,8140 8041,8010 8503,8050 8041,8010 8503,8500 8523,8500 8500,8520
      // Histo +     8041,8246 8240,8000 8500,8503 8500,8503 8041,8000 8500,8520 8140,8500 8480,8500 8500,8520 8041,8140 8071,8070 8500,8520 8500,8520 8041,8000 8500,8520 8140,8500 8575,8500 8500,8503 8000,8140 8500,8520 8041,8000 8500,8520 8041,8000 8500,8520 8500,8000 8500,8520 8500,8520 8041,8000 8575,8500 8041,8046 8012,8000 8500,8520 8010,8000 8522,8500 8041,8000 8046,8000 8041,8000 8500,8000 8046,8000 8033,8032 9590,X 8500,8503 8500,8000
      static private final class HistoComparator implements Comparator<String> {
         public int compare( final String histo1, final String histo2 ) {
            final List<String> HISTO_ORDER
//               = Arrays.asList( "8070", "8520", "8503", "8500", "8260", "8250", "8140", "8480", "8046", "8000", "8010" );
                  = Arrays.asList( "8071", "8070", "8520", "8575", "8500", "8503", "8260", "8250", "8140", "8480",
                  "8046", "8041", "8240", "8012", "8000", "8010" );
            if ( !histo1.equals( histo2 ) ) {
               for ( String order : HISTO_ORDER ) {
                  if ( histo1.equals( order ) ) {
                     return 1;
                  } else if ( histo2.equals( order ) ) {
                     return -1;
                  }
               }
            }
            return String.CASE_INSENSITIVE_ORDER.compare( histo1, histo2 );
         }
      }


   }
