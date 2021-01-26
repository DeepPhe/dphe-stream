//package org.healthnlp.deepphe.summary.attribute;
//
//import org.healthnlp.deepphe.core.neo4j.Neo4jOntologyConceptUtil;
//import org.healthnlp.deepphe.neo4j.constant.RelationConstants;
//import org.healthnlp.deepphe.neo4j.constant.UriConstants;
//import org.healthnlp.deepphe.neo4j.embedded.EmbeddedConnection;
//import org.healthnlp.deepphe.neo4j.node.Mention;
//import org.healthnlp.deepphe.neo4j.node.NeoplasmAttribute;
//import org.healthnlp.deepphe.summary.concept.ConceptAggregate;
//import org.healthnlp.deepphe.summary.engine.NeoplasmSummaryCreator;
//import org.healthnlp.deepphe.util.KeyValue;
//import org.healthnlp.deepphe.util.TopoMorphValidator;
//import org.neo4j.graphdb.GraphDatabaseService;
//
//import java.util.*;
//import java.util.function.Consumer;
//import java.util.function.Function;
//import java.util.function.Predicate;
//import java.util.stream.Collectors;
//
//import static org.healthnlp.deepphe.summary.attribute.SpecificAttribute.EvidenceLevel.*;
//import static org.healthnlp.deepphe.summary.attribute.util.AddFeatureUtil.*;
//
//
//
//final public class MajorTopography implements SpecificAttribute {
//
//   private String _bestSiteUri = "";
//   private Collection<String> _bestAssociatedSiteUris;
//   final private Collection<String> _topographyCodes = new HashSet<>();
//   final private NeoplasmAttribute _neoplasmAttribute;
//   private String _majorTopoCode;
//
//
//   public MajorTopography( final ConceptAggregate neoplasm,
//                           final Collection<ConceptAggregate> allConcepts ) {
//      _neoplasmAttribute = createMajorTopoAttribute( neoplasm, allConcepts );
//   }
//
//   public NeoplasmAttribute toNeoplasmAttribute() {
//      return _neoplasmAttribute;
//   }
//
//   public Collection<String> getTopographyCodes() {
//      return _topographyCodes;
//   }
//
//   static private String getMajorTopoCode( final Collection<String> topographyCodes ) {
//      if ( topographyCodes.isEmpty() ) {
//         return "C80";
//      }
//      final Function<String, String> getMajorCode = t -> {
//         final int dot = t.indexOf( '.' );
//         return dot > 0 ? t.substring( 0, dot ) : t;
//      };
////      return topographyCodes.stream()
////                           .map( getMajorCode )
////                           .distinct()
////                           .sorted()
////                           .collect( Collectors.joining( ";" ) );
//      final List<String> codes = topographyCodes.stream()
//                            .map( getMajorCode )
//                            .distinct()
//                            .sorted()
//                            .collect( Collectors.toList() );
//      return codes.get( codes.size()-1 );
//   }
//
//   public String getBestSiteUri() {
//      return _bestSiteUri;
//   }
//
//   public Collection<String> getBestAssociatedSiteUris() {
//      return _bestAssociatedSiteUris;
//   }
//
//   private NeoplasmAttribute createMajorTopoAttribute( final ConceptAggregate neoplasm,
//                                                     final Collection<ConceptAggregate> allConcepts ) {
//      new SiteAttributeHelper( neoplasm, allConcepts );
//
//
//      final SiteFeatureHelper featureHelper = new SiteFeatureHelper( neoplasm, allConcepts );
//      _bestSiteUri = featureHelper.getBestUri();
//
//      if ( _bestSiteUri == null || _bestSiteUri.isEmpty() || _bestSiteUri.equals( "DeepPhe" ) ) {
//         return SpecificAttribute.createAttribute( "topography_major",
//                                                   "C80",
//                                                   Collections.emptyList(),
//                                                   Collections.emptyList(),
//                                                   Collections.emptyList(),
//                                                   createEmptyFeatures() );
//      }
//
//      _bestAssociatedSiteUris = featureHelper.getAllBestAssociatedUris();
//      final List<Integer> features = createFeatures( featureHelper );
//
//      final Map<EvidenceLevel,Collection<Mention>> evidence = mapEvidence( featureHelper.getAllBestAssociatedUris(),
//                                                                     neoplasm.getRelatedSites() );
//      return SpecificAttribute.createAttribute( "topography_major",
//                                                _majorTopoCode,
//                                                new ArrayList<>( evidence.get( DIRECT_EVIDENCE ) ),
//                                                new ArrayList<>( evidence.get( INDIRECT_EVIDENCE ) ),
//                                                new ArrayList<>( evidence.get( NOT_EVIDENCE ) ),
//                                                features );
//   }
//
//   static private Map<EvidenceLevel,Collection<Mention>> mapEvidence( final Collection<String> firstSiteUris,
//                                                                final Collection<ConceptAggregate> siteConcepts ) {
//      final Map<EvidenceLevel,Collection<Mention>> evidenceMap = new HashMap<>();
//
//      final Function<ConceptAggregate,KeyValue<EvidenceLevel,Collection<Mention>>> splitMentions = c ->
//         firstSiteUris.contains( c.getUri() )
//         ? new KeyValue<>( DIRECT_EVIDENCE, c.getMentions() )
//         : new KeyValue<>( INDIRECT_EVIDENCE, c.getMentions() );
//
//      final Consumer<KeyValue<EvidenceLevel,Collection<Mention>>> placeMentions = kv ->
//            evidenceMap.computeIfAbsent( kv.getKey(), v -> new HashSet<>() ).addAll( kv.getValue() );
//
//      evidenceMap.put( DIRECT_EVIDENCE, new HashSet<>() );
//      evidenceMap.put( INDIRECT_EVIDENCE, new HashSet<>() );
//      evidenceMap.put( NOT_EVIDENCE, new HashSet<>() );
//      siteConcepts.stream()
//                  .map( splitMentions )
//                  .forEach( placeMentions );
//      return evidenceMap;
//   }
//
//
//   private List<Integer> createFeatures( final SiteFeatureHelper featureHelper ) {
//      final SiteInfoStore site = new SiteInfoStore( featureHelper );
//         //1.  !!!!!  Individual URI  !!!!!
//         //    ======  URI  =====
//         //1.  1 = # site uri in neoplasm / # site uris in neoplasm  (v first order)
//         //2.  1 = # site uri in neoplasm / # site uris in neoplasm  (v any order)
//         //3.  1 = # site uri in neoplasm/patient / # site uris in patient	(v patient)
//      final int feature1 = scoreInt0to10( 1, site._allFirstSiteUris.size() );
//      final int feature2 = scoreInt0to10( 1, site._allNeoplasmSiteUris.size() );
//      final int feature3 = scoreInt0to10( 1, site._allPatientSiteUris.size() );
//         //    ======  CONCEPT  =====
//         //4. # concepts with site uri in neoplasm / # site concepts in neoplasm  (first order v first order)
//         //5. # concepts with site uri in neoplasm / # site concepts in neoplasm  (first order v any order)
//         //6. # concepts with site uri in neoplasm / # site concepts in neoplasm  (any order v any order)
//         //7. # concepts with site uri in neoplasm / # site concepts in patient  (first order v patient)
//         //8. # concepts with site uri in neoplasm / # site concepts in patient  (any order v patient)
//         //9. # concepts with site uri in patient / # site concepts in patient  (patient v patient)
//      final int bestInFirstSiteCount = site._bestInFirstSites.size();
//      final int bestInNeoplasmSiteCount = site._bestInNeoplasmSites.size();
//      final int bestInPatientSiteCount = site._bestInPatientSites.size();
//      final int allNeoplasmSiteCount = featureHelper.getNeoplasmSites().size();
//      final int allPatientSiteCount = featureHelper.getPatientSites().size();
//      final int feature4 = scoreInt0to10( bestInFirstSiteCount, featureHelper.getFirstSites().size() );
//      final int feature5 = scoreInt0to10( bestInFirstSiteCount, allNeoplasmSiteCount );
//      final int feature6 = scoreInt0to10( bestInNeoplasmSiteCount, allNeoplasmSiteCount );
//      final int feature7 = scoreInt0to10( bestInFirstSiteCount, allPatientSiteCount );
//      final int feature8 = scoreInt0to10( bestInNeoplasmSiteCount, allPatientSiteCount );
//      final int feature9 = scoreInt0to10( bestInPatientSiteCount, allPatientSiteCount );
//         //    ======  MENTION  =====
//         //10. # mentions with exact site uri in neoplasm / # exact site mentions in neoplasm  (first order v first order)
//         //11. # mentions with site uri in neoplasm / # site mentions in neoplasm  (first order v any order)
//         //12. # mentions with site uri in neoplasm / # site mentions in neoplasm  (any order v any order)
//         //13. # mentions with site uri in neoplasm / # site mentions in patient  (first order v patient)
//         //14. # mentions with site uri in neoplasm / # site mentions in patient  (any order v patient)
//         //15. # mentions with site uri in patient / # site mentions in patient  (patient v patient)
//      final int bestInFirstMentionCount = site._bestInFirstMentions.size();
//      final int bestInNeoplasmMentionCount = site._bestInNeoplasmMentions.size();
//      final int bestInPatientMentionCount = site._bestInPatientMentions.size();
//      final int allNeoplasmMentionCount = featureHelper.getNeoplasmMentions().size();
//      final int allPatientMentionCount = featureHelper.getPatientMentions().size();
//      final int feature10 = scoreInt0to10( bestInFirstMentionCount, featureHelper.getFirstMentions().size() );
//      final int feature11 = scoreInt0to10( bestInFirstMentionCount, allNeoplasmMentionCount );
//      final int feature12 = scoreInt0to10( bestInNeoplasmMentionCount, allNeoplasmMentionCount );
//      final int feature13 = scoreInt0to10( bestInFirstMentionCount, allPatientMentionCount );
//      final int feature14 = scoreInt0to10( bestInNeoplasmMentionCount, allPatientMentionCount );
//      final int feature15 = scoreInt0to10( bestInPatientMentionCount, allPatientMentionCount );
//
//         //2.  !!!!!  URI Branch  !!!!!
//         //    ======  URI  =====
//         //16.  1 = # site uri branch in neoplasm / # site uri branches in neoplasm  (v first order)
//         //17.  1 = # site uri branch neoplasm / # site uri branches in neoplasm  (v any order)
//         //18.  1 = # site uri branch in neoplasm/patient / # site uri branches in patient	(v patient)
//      final int feature16 = scoreInt0to10( 1, site._firstMentionBranchCounts.size() );
//      final int feature17 = scoreInt0to10( 1, site._neoplasmMentionBranchCounts.size() );
//      final int feature18 = scoreInt0to10( 1, site._patientMentionBranchCounts.size() );
//         //    ======  CONCEPT  =====
//         //19. # concepts in site uri branch in neoplasm / # concepts in site uri branches in neoplasm  (first order v first order)
//         //20. # concepts in site uri branch in neoplasm / # concepts in site uri branches in neoplasm  (first order v any order)
//         //21. # concepts in site uri branch in neoplasm / # concepts in site uri branches in neoplasm  (any order v any order)
//         //22. # concepts in site uri branch in neoplasm / # concepts in site uri branches in patient  (first order v patient)
//         //23. # concepts in site uri branch in neoplasm / # concepts in site uri branches in patient  (any order v patient)
//         //??. # concepts in site uri branch in patient / # concepts in site uri branches in patient  (patient v patient)  ?Too much?
//      final int feature19 = scoreInt0to10( site._bestFirstConceptBranchCount,
//                                           site._firstConceptBranchCount );
//      final int feature20 = scoreInt0to10( site._bestFirstConceptBranchCount,
//                                           site._neoplasmConceptBranchCount );
//      final int feature21 = scoreInt0to10( site._bestNeoplasmConceptBranchCount,
//                                           site._neoplasmConceptBranchCount );
//      final int feature22 = scoreInt0to10( site._bestFirstConceptBranchCount,
//                                           site._patientConceptBranchCount );
//      final int feature23 = scoreInt0to10( site._bestNeoplasmConceptBranchCount,
//                                           site._patientConceptBranchCount );
//         //    ======  MENTION  =====
//         //24. # mentions in site uri branch in neoplasm / # mentions in site uri branches in neoplasm  (first order v first order)
//         //25. # mentions in site uri branch in neoplasm / # mentions in site uri branches in neoplasm  (first order v any order)
//         //26. # mentions in site uri branch in neoplasm / # mentions in site uri branches in neoplasm  (any order v any order)
//         //27. # mentions in site uri branch in neoplasm / # mentions in site uri branches in patient  (first order v patient)
//         //28. # mentions in site uri branch in neoplasm / # mentions in site uri branches in patient  (any order v patient)
//         //??. # mentions in site uri branch in patient / # mentions in site uri branches in patient  (patient v patient)  ?Too much?
//      final int feature24 = scoreInt0to10( site._bestFirstMentionBranchCount,
//                                           site._firstMentionBranchCount );
//      final int feature25 = scoreInt0to10( site._bestFirstMentionBranchCount,
//                                           site._neoplasmMentionBranchCount );
//      final int feature26 = scoreInt0to10( site._bestNeoplasmMentionBranchCount,
//                                           site._neoplasmMentionBranchCount );
//      final int feature27 = scoreInt0to10( site._bestFirstMentionBranchCount,
//                                           site._patientMentionBranchCount );
//      final int feature28 = scoreInt0to10( site._bestNeoplasmMentionBranchCount,
//                                           site._patientMentionBranchCount );
//
//         //3.  !!!!!  URI Depth  !!!!!
//         //    ======  URI  =====
//         //29. class depth site uri
//         //30. class depth site uri / greatest class depth site uris in neoplasm  (v first order)
//         //31. class depth site uri / greatest class depth site uris in neoplasm  (v any order)
//         //32. class depth site uri / greatest class depth site uris in patient  (v patient)
//      final int feature29 = Math.min( 10, site._bestMaxDepth * 2 );
//      final int feature30 = scoreInt0to10( site._bestFirstConceptBranchCount, site._firstMaxDepth );
//      final int feature31 = scoreInt0to10( site._bestFirstConceptBranchCount, site._neoplasmMaxDepth );
//      final int feature32 = scoreInt0to10( site._bestFirstConceptBranchCount, site._patientMaxDepth );
//
//         //4.  !!!!!  Relation Count  !!!!!
//         //33. # "HAS_SITE" relations for site uri / # total "HAS_SITE" relations for all uris (v neoplasm)
//         //34. # "HAS_SITE" relations for site uri / # total "HAS_SITE" relations for patient  (v patient)
//      final int feature33 = scoreInt0to10( site._bestRelationCount, site._allSiteRelationCount );
//      final int feature34 = scoreInt0to10( site._bestRelationCount, site._patientRelationCount );
//
//         //5.  !!!!!  Runner-Up  !!!!!
//         //    ======  URI  =====
//         //-
//         //    ======  CONCEPT  =====
//         //35. # runner-up concepts with site uri in neoplasm / # winner site concepts in neoplasm  (first order v first order)
//         //36. # runner-up concepts with site uri in neoplasm / # winner site concepts in neoplasm  (first order v any order)
//         //??. # runner-up concepts with site uri in neoplasm / # winner site concepts in neoplasm  (any order v any order)  - Could be a different runner-up, could be > 1
//         //37. # runner-up concepts with site uri in neoplasm / # winner site concepts in patient  (first order v patient)
//         //??. # runner-up concepts with site uri in neoplasm / # winner site concepts in patient  (any order v patient)  - Could be a different runner-up, could be > 1
//         //??. # runner-up concepts with site uri in patient / # winner site concepts in patient  (patient v patient)  - Could be a different runner-up, could be > 1
//      final int feature35 = scoreInt0to10( site._runnerUpFirstConceptCount, site._bestFirstConceptBranchCount );
//      final int feature36 = scoreInt0to10( site._runnerUpFirstConceptCount, site._bestNeoplasmConceptBranchCount );
//      final int feature36_1 = scoreInt0to10( site._runnerUpNeoplasmConceptCount,
//                                             site._bestNeoplasmConceptBranchCount );
//      final int feature37 = scoreInt0to10( site._runnerUpFirstConceptCount, site._bestInPatientSites.size() );
//      final int feature37_1 = scoreInt0to10( site._runnerUpNeoplasmConceptCount,
//                                             site._bestInPatientSites.size() );
//      final int feature37_2 = scoreInt0to10( site._runnerUpPatientConceptCount,
//                                             site._bestInPatientSites.size() );
//
//         //    ======  MENTION  =====
//         //38. # runner-up mentions with site uri in neoplasm / # winner site mentions in neoplasm  (first order v first order)
//         //39. # runner-up mentions with site uri in neoplasm / # winner site mentions in neoplasm  (first order v any order)
//         //??. # runner-up mentions with site uri in neoplasm / # winner site mentions in neoplasm  (any order v any order)  - Could be a different runner-up, could be > 1
//         //40. # runner-up mentions with site uri in neoplasm / # winner site mentions in patient  (first order v patient)
//         //??. # runner-up mentions with site uri in neoplasm / # winner site mentions in patient  (any order v patient)  - Could be a different runner-up, could be > 1
//         //??. # runner-up mentions with site uri in patient / # winner site mentions in patient  (patient v patient)  - Could be a different runner-up, could be > 1
//      final int feature38 = scoreInt0to10( site._runnerUpFirstMentionCount, site._bestFirstMentionBranchCount );
//      final int feature39 = scoreInt0to10( site._runnerUpFirstMentionCount, site._bestNeoplasmMentionBranchCount );
//      final int feature39_1 = scoreInt0to10( site._runnerUpNeoplasmMentionCount,
//                                             site._bestNeoplasmMentionBranchCount );
//      final int feature40 = scoreInt0to10( site._runnerUpFirstMentionCount, site._bestInPatientMentions.size() );
//      final int feature40_1 = scoreInt0to10( site._runnerUpNeoplasmMentionCount,
//                                             site._bestInPatientMentions.size() );
//      final int feature40_2 = scoreInt0to10( site._runnerUpPatientMentionCount,
//                                             site._bestInPatientMentions.size() );
//
//         //6.  !!!!!  Relation Depth  !!!!!
//         //    =====  Relation Depth  =====
//         //41.  First Order Depth - {1-5}
//         //42.  2nd Order Depth - {0,2-5}
//         //43. # Depths occupied - {1-5}
//         //44. # Depths occupied by site uri - {1-5}
//         //45. # Depths occupied by site uri branch - {1-5}
//      final List<Integer> orderDepths = featureHelper.getSiteRelationOrders();
//      final int feature41 = orderDepths.isEmpty() ? 0 : orderDepths.get( 0 ) * 2;
//      final int feature42 = orderDepths.size() <= 1
//                              ? 0
//                              : orderDepths.get( 1 ) * 2;
//      final int feature43 = orderDepths.size() * 2;
//      final int feature44 = featureHelper.getOrdersOccupied( featureHelper.getAllBestAssociatedUris() )
//                                                   .size() * 2;
//      final int feature45 = 2 *
//            featureHelper.getOrdersOccupied( featureHelper.mapAllUriRoots( site._bestInFirstSites )
//                                                          .values()
//                                                          .stream()
//                                                          .flatMap( Collection::stream )
//                                                          .collect( Collectors.toList() ) )
//                                                   .size();
//         //7.  !!!!!  Topography Codes  !!!!!
//         //    =====  Ontology  =====
//         //46. # ontology topography codes
//         //47. # lookup table topography codes
//         //47XX. # topography codes for site uri branch  - Could be 0
//         //??. # topography codes for site uri branch / # topography codes for uris  (v first order)
//         //??. # topography codes for site uri branch / # topography codes for uris  (v any order)
//         //
//         //    =====  Lookup Table  =====
//         //48XX. topography code for site uri -  {0,1}
//         //49XX. # topography codes for site uri branch  - Could be 0
//      final Collection<String> ontoTopoCodes = getOntoTopoCodes( featureHelper.getAllBestAssociatedUris() );
//      final int feature46 = ontoTopoCodes.size();
//      final Collection<String> tableTopoCodes = getTableTopoCodes( featureHelper.getAllBestAssociatedUris() );
//      final int feature47 = tableTopoCodes.size();
//      final Collection<String> allTopoCodes = new HashSet<>( ontoTopoCodes );
//      allTopoCodes.addAll( tableTopoCodes );
//      int feature48 = 0;
//      int feature49 = 0;
//      _topographyCodes.addAll( allTopoCodes );
//      _majorTopoCode = getMajorTopoCode( _topographyCodes );
//      if ( !allTopoCodes.isEmpty() ) {
//         final Collection<String> firstOtherSiteUris = new HashSet<>( site._allFirstSiteUris );
//         firstOtherSiteUris.removeAll( featureHelper.getAllBestAssociatedUris() );
//         final Collection<String> firstOtherTopoCodes = new HashSet<>();
//         if ( !firstOtherSiteUris.isEmpty() ) {
//            firstOtherTopoCodes.addAll( getOntoTopoCodes( firstOtherSiteUris ) );
//            firstOtherTopoCodes.addAll( getTableTopoCodes( firstOtherSiteUris ) );
//            feature48 = scoreInt0to10( allTopoCodes.size(),
//                                         firstOtherTopoCodes.size() + allTopoCodes.size() );
//         } else {
//            feature48 = 1;
//         }
//         final Collection<String> neoplasmOtherSiteUris = new HashSet<>( site._allNeoplasmSiteUris );
//         neoplasmOtherSiteUris.removeAll( featureHelper.getAllBestAssociatedUris() );
//         neoplasmOtherSiteUris.removeAll( firstOtherSiteUris );
//         if ( !neoplasmOtherSiteUris.isEmpty() ) {
//            final Collection<String> neoplasmOtherTopoCodes =
//                  new HashSet<>( getOntoTopoCodes( neoplasmOtherSiteUris ) );
//            neoplasmOtherTopoCodes.addAll( getTableTopoCodes( neoplasmOtherSiteUris ) );
//            feature49 = scoreInt0to10( allTopoCodes.size(),
//                                         neoplasmOtherTopoCodes.size()
//                                         + firstOtherTopoCodes.size() + allTopoCodes.size() );
//         } else {
//            feature49 = 1;
//         }
//      }
//
//         //    =====  Code Sorting  =====
//         //50XX. Manual Sorting Applied -  {0,1}  --> useful for morph but not topo.
//      final List<Integer> features = new ArrayList<>();
//      features.add( feature1 );
//      features.add( feature2 );
//      features.add( feature3 );
//      features.add( feature4 );
//      features.add( feature5 );
//      features.add( feature6 );
//      features.add( feature7 );
//      features.add( feature8 );
//      features.add( feature9 );
//      features.add( feature10 );
//      features.add( feature11 );
//      features.add( feature12 );
//      features.add( feature13 );
//      features.add( feature14 );
//      features.add( feature15 );
//      features.add( feature16 );
//      features.add( feature17 );
//      features.add( feature18 );
//      features.add( feature19 );
//      features.add( feature20 );
//      features.add( feature21 );
//      features.add( feature22 );
//      features.add( feature23 );
//      features.add( feature24 );
//      features.add( feature25 );
//      features.add( feature26 );
//      features.add( feature27 );
//      features.add( feature28 );
//      features.add( feature29 );
//      features.add( feature30 );
//      features.add( feature31 );
//      features.add( feature32 );
//      features.add( feature33 );
//      features.add( feature34 );
//      features.add( feature35 );
//      features.add( feature36 );
//      features.add( feature36_1 );
//      features.add( feature37 );
//      features.add( feature37_1 );
//      features.add( feature37_2 );
//      features.add( feature38 );
//      features.add( feature39 );
//      features.add( feature39_1 );
//      features.add( feature40 );
//      features.add( feature40_1 );
//      features.add( feature40_2 );
//      features.add( feature41 );
//      features.add( feature42 );
//      features.add( feature43 );
//      features.add( feature44 );
//      features.add( feature45 );
//      features.add( feature46 );
//      features.add( feature47 );
//      features.add( feature48 );
//      features.add( feature49 );
//
//      features.add( Math.min( 10, bestInNeoplasmSiteCount ) );
//      features.add( Math.min( 10, bestInPatientSiteCount ) );
//      features.add( Math.min( 10, bestInNeoplasmMentionCount ) );
//      features.add( Math.min( 10, bestInPatientMentionCount ) );
//      features.add( Math.min( 10, site._bestNeoplasmMentionBranchCount ) );
//      features.add( _majorTopoCode.equals( "C80" ) ? 0 : 10 );
//      features.add( featureHelper.getNeoplasm().isNegated() ? 0 : 10 );
//      features.add( featureHelper.getNeoplasm().isUncertain() ? 0 : 10 );
//      features.add( featureHelper.getNeoplasm().isGeneric() ? 0 : 10 );
//      features.add( featureHelper.getNeoplasm().isConditional() ? 0 : 10 );
//      features.add( Math.min( 10, featureHelper.getNeoplasm().getMentions().size() ) );
//      final Collection<String> morphs = NeoplasmSummaryCreator.getMorphology( featureHelper.getNeoplasm(),
//                                                                              _majorTopoCode + "0" );
//      features.add( NeoplasmSummaryCreator.getBestHistology( morphs ).equals( "8000" ) ? 0 : 10 );
//
//      features.add( Math.min( 10, (site._bestNeoplasmMentionBranchCount+1) * site._bestNeoplasmMentionBranchCount ) );
//
//
//
//
//      return features;
//   }
//
//
//
//   static private Collection<String> getOntoTopoCodes( final Collection<String> uris ) {
//      return uris.stream()
//                  .map( Neo4jOntologyConceptUtil::getIcdoTopoCode )
//                  .filter( t -> !t.isEmpty() )
//                  .collect( Collectors.toSet() );
//   }
//
//   static private Collection<String> getTableTopoCodes( final Collection<String> uris ) {
//      return uris.stream()
//                   .map( TopoMorphValidator.getInstance()::getSiteCode )
//                   .filter( t -> !t.isEmpty() )
//                   .collect( Collectors.toSet() );
//   }
//
//
//   static private Map<String,Integer> mapUriMentionCounts( final Collection<ConceptAggregate> concepts ) {
//      final Map<String,Integer> uriMentionCounts = new HashMap<>();
//      for ( ConceptAggregate concept : concepts ) {
//         concept.getUriMentions()
//                .forEach( (k,v) -> uriMentionCounts.put( k, uriMentionCounts.getOrDefault( k, 0 ) + v.size() ) );
//      }
//      return uriMentionCounts;
//   }
//
//
//
//   static private List<Mention> listPatientSites( final Collection<ConceptAggregate> allConcepts ) {
//      final GraphDatabaseService graphDb = EmbeddedConnection.getInstance()
//                                                             .getGraph();
//      return allConcepts.stream()
//                        .filter( c -> UriConstants.getLocationUris( graphDb ).contains( c.getUri() ) )
//                        .map( ConceptAggregate::getMentions )
//                        .flatMap( Collection::stream )
//                        .collect( Collectors.toList() );
//
//   }
//
//   static private int countExactSiteMentions( final String bestSiteUri,
//                                              final List<Mention> directEvidence ) {
//      return (int)directEvidence.stream()
//                          .filter( m -> bestSiteUri.equals( m.getClassUri() ) )
//                          .count();
//   }
//
//
//
//
//
//
//   static private class SiteInfoStore {
//      private final Collection<String> _allFirstSiteUris;
//      private final Collection<String> _allNeoplasmSiteUris;
//      private final Collection<String> _allPatientSiteUris;
//
//      private final Collection<ConceptAggregate> _bestInFirstSites;
//      private final Collection<ConceptAggregate> _bestInNeoplasmSites;
//      private final Collection<ConceptAggregate> _bestInPatientSites;
//
//      private final Collection<Mention> _bestInFirstMentions;
//      private final Collection<Mention> _bestInNeoplasmMentions;
//      private final Collection<Mention> _bestInPatientMentions;
//
//      private final Map<String,Integer> _firstMentionBranchCounts;
//      private final Map<String,Integer> _neoplasmMentionBranchCounts;
//      private final Map<String,Integer> _patientMentionBranchCounts;
//
//      private final Map<String,Integer> _bestFirstConceptBranchCounts;
//
//      private final int _bestFirstConceptBranchCount;
//      private final int _bestNeoplasmConceptBranchCount;
//      private final int _firstConceptBranchCount;
//      private final int _neoplasmConceptBranchCount;
//      private final int _patientConceptBranchCount;
//
//      private final Map<String,Integer> _bestFirstMentionBranchCounts;
//
//      private final int _bestFirstMentionBranchCount;
//      private final int _bestNeoplasmMentionBranchCount;
//      private final int _firstMentionBranchCount;
//      private final int _neoplasmMentionBranchCount;
//      private final int _patientMentionBranchCount;
//
//      private final int _bestMaxDepth;
//      private final int _firstMaxDepth;
//      private final int _neoplasmMaxDepth;
//      private final int _patientMaxDepth;
//
//      private final int _bestRelationCount;
//      private final int _allSiteRelationCount;
//      private final int _patientRelationCount;
//
//      private final int _runnerUpFirstConceptCount;
//      private final int _runnerUpNeoplasmConceptCount;
//      private final int _runnerUpPatientConceptCount;
//
//      private final int _runnerUpFirstMentionCount;
//      private final int _runnerUpNeoplasmMentionCount;
//      private final int _runnerUpPatientMentionCount;
//
//      private SiteInfoStore( final SiteFeatureHelper featureHelper ) {
//         //1.  !!!!!  Individual URI  !!!!!
//         //    ======  URI  =====
//         //1.  1 = # site uri in neoplasm / # site uris in neoplasm  (v first order)
//         //2.  1 = # site uri in neoplasm / # site uris in neoplasm  (v any order)
//         //3.  1 = # site uri in neoplasm/patient / # site uris in patient	(v patient)
//         _allFirstSiteUris = featureHelper.getFirstUris();
//         _allNeoplasmSiteUris = featureHelper.getNeoplasmUris();
//         _allPatientSiteUris = featureHelper.getPatientUris();
//         //    ======  CONCEPT  =====
//         //4. # concepts with site uri in neoplasm / # site concepts in neoplasm  (first order v first order)
//         //5. # concepts with site uri in neoplasm / # site concepts in neoplasm  (first order v any order)
//         //6. # concepts with site uri in neoplasm / # site concepts in neoplasm  (any order v any order)
//         //7. # concepts with site uri in neoplasm / # site concepts in patient  (first order v patient)
//         //8. # concepts with site uri in neoplasm / # site concepts in patient  (any order v patient)
//         //9. # concepts with site uri in patient / # site concepts in patient  (patient v patient)
//         _bestInFirstSites = featureHelper.getHasBestUriSites( featureHelper.getFirstSites() );
//         _bestInNeoplasmSites = featureHelper.getHasBestUriSites( featureHelper.getNeoplasmSites() );
//         _bestInPatientSites = featureHelper.getHasBestUriSites( featureHelper.getPatientSites() );
//         //    ======  MENTION  =====
//         //10. # mentions with site uri in neoplasm / # site mentions in neoplasm  (first order v first order)
//         //11. # mentions with site uri in neoplasm / # site mentions in neoplasm  (first order v any order)
//         //12. # mentions with site uri in neoplasm / # site mentions in neoplasm  (any order v any order)
//         //13. # mentions with site uri in neoplasm / # site mentions in patient  (first order v patient)
//         //14. # mentions with site uri in neoplasm / # site mentions in patient  (any order v patient)
//         //15. # mentions with site uri in patient / # site mentions in patient  (patient v patient)
//         _bestInFirstMentions
//               = featureHelper.getExactBestUriMentions( featureHelper.getFirstSites() );
//         _bestInNeoplasmMentions
//               = featureHelper.getExactBestUriMentions( featureHelper.getNeoplasmSites() );
//         _bestInPatientMentions
//               = featureHelper.getExactBestUriMentions( featureHelper.getPatientSites() );
//
//         //2.  !!!!!  URI Branch  !!!!!
//         //    ======  URI  =====
//         //16.  1 = # site uri branch in neoplasm / # site uri branches in neoplasm  (v first order)
//         //17.  1 = # site uri branch neoplasm / # site uri branches in neoplasm  (v any order)
//         //18.  1 = # site uri branch in neoplasm/patient / # site uri branches in patient	(v patient)
//         _firstMentionBranchCounts =
//               featureHelper.mapUriBranchMentionCounts( featureHelper.getFirstSites() );
//         _neoplasmMentionBranchCounts =
//               featureHelper.mapUriBranchMentionCounts( featureHelper.getNeoplasmSites() );
//         _patientMentionBranchCounts =
//               featureHelper.mapUriBranchMentionCounts( featureHelper.getPatientSites() );
//         //    ======  CONCEPT  =====
//         //19. # concepts in site uri branch in neoplasm / # concepts in site uri branches in neoplasm  (first order v first order)
//         //20. # concepts in site uri branch in neoplasm / # concepts in site uri branches in neoplasm  (first order v any order)
//         //21. # concepts in site uri branch in neoplasm / # concepts in site uri branches in neoplasm  (any order v any order)
//         //22. # concepts in site uri branch in neoplasm / # concepts in site uri branches in patient  (first order v patient)
//         //23. # concepts in site uri branch in neoplasm / # concepts in site uri branches in patient  (any order v patient)
//         //??. # concepts in site uri branch in patient / # concepts in site uri branches in patient  (patient v patient)  ?Too much?
//         _bestFirstConceptBranchCounts =
//               featureHelper.mapUriBranchConceptCounts( _bestInFirstSites );
//         final Map<String, Integer> _bestNeoplasmConceptBranchCounts = featureHelper.mapUriBranchConceptCounts(
//               _bestInNeoplasmSites );
//         final Map<String, Integer> _firstConceptBranchCounts = featureHelper.mapUriBranchConceptCounts(
//               featureHelper.getFirstSites() );
//         final Map<String, Integer> _neoplasmConceptBranchCounts = featureHelper.mapUriBranchConceptCounts(
//               featureHelper.getNeoplasmSites() );
//         final Map<String, Integer> _patientConceptBranchCounts = featureHelper.mapUriBranchConceptCounts(
//               featureHelper.getPatientSites() );
//         _bestFirstConceptBranchCount = _bestFirstConceptBranchCounts.values().stream().mapToInt( i -> i ).sum();
//         _bestNeoplasmConceptBranchCount = _bestNeoplasmConceptBranchCounts.values().stream().mapToInt( i -> i ).sum();
//         _firstConceptBranchCount = _firstConceptBranchCounts.values().stream().mapToInt( i -> i ).sum();
//         _neoplasmConceptBranchCount = _neoplasmConceptBranchCounts.values().stream().mapToInt( i -> i ).sum();
//         _patientConceptBranchCount = _patientConceptBranchCounts.values().stream().mapToInt( i -> i ).sum();
//         //    ======  MENTION  =====
//         //24. # mentions in site uri branch in neoplasm / # mentions in site uri branches in neoplasm  (first order v first order)
//         //25. # mentions in site uri branch in neoplasm / # mentions in site uri branches in neoplasm  (first order v any order)
//         //26. # mentions in site uri branch in neoplasm / # mentions in site uri branches in neoplasm  (any order v any order)
//         //27. # mentions in site uri branch in neoplasm / # mentions in site uri branches in patient  (first order v patient)
//         //28. # mentions in site uri branch in neoplasm / # mentions in site uri branches in patient  (any order v patient)
//         //??. # mentions in site uri branch in patient / # mentions in site uri branches in patient  (patient v patient)  ?Too much?
//         _bestFirstMentionBranchCounts =
//               featureHelper.mapUriBranchMentionCounts( _bestInFirstSites );
//         final Map<String, Integer> _bestNeoplasmMentionBranchCounts = featureHelper.mapUriBranchMentionCounts(
//               featureHelper.getHasBestUriSites( featureHelper.getNeoplasmSites() ) );
//         _bestFirstMentionBranchCount = _bestFirstMentionBranchCounts.values().stream().mapToInt( i -> i ).sum();
//         _bestNeoplasmMentionBranchCount = _bestNeoplasmMentionBranchCounts.values().stream().mapToInt( i -> i ).sum();
//         _firstMentionBranchCount = _firstMentionBranchCounts.values().stream().mapToInt( i -> i ).sum();
//         _neoplasmMentionBranchCount = _neoplasmMentionBranchCounts.values().stream().mapToInt( i -> i ).sum();
//         _patientMentionBranchCount = _patientMentionBranchCounts.values().stream().mapToInt( i -> i ).sum();
//
//         //3.  !!!!!  URI Depth  !!!!!
//         //    ======  URI  =====
//         //29. class depth site uri
//         //30. class depth site uri / greatest class depth site uris in neoplasm  (v first order)
//         //31. class depth site uri / greatest class depth site uris in neoplasm  (v any order)
//         //32. class depth site uri / greatest class depth site uris in patient  (v patient)
//         final Map<String,Integer> classLevelMap
//               = _allPatientSiteUris.stream()
//                                   .collect( Collectors.toMap( Function.identity(),
//                                                               Neo4jOntologyConceptUtil::getClassLevel ) );
//         final Collection<String> allBestUris = featureHelper.getAllBestAssociatedUris();
//         _bestMaxDepth = allBestUris.stream()
//                                             .mapToInt( u ->  classLevelMap.getOrDefault( u, 0 ) )
//                                             .max()
//                                             .orElse( 0 );
//         _firstMaxDepth = _allFirstSiteUris.stream()
//                                                   .mapToInt( u ->  classLevelMap.getOrDefault( u, 0 ) )
//                                                   .max()
//                                                   .orElse( 0 );
//         _neoplasmMaxDepth = _allNeoplasmSiteUris.stream()
//                                                         .mapToInt( u ->  classLevelMap.getOrDefault( u, 0 ) )
//                                                         .max()
//                                                         .orElse( 0 );
//         _patientMaxDepth = classLevelMap.values().stream()
//                                                  .mapToInt( i -> i )
//                                                  .max()
//                                                  .orElse( 0 );
//         //4.  !!!!!  Relation Count  !!!!!
//         //33. # "HAS_SITE" relations for site uri / # total "HAS_SITE" relations for all uris (v neoplasm)
//         //34. # "HAS_SITE" relations for site uri / # total "HAS_SITE" relations for patient  (v patient)
//         final Predicate<ConceptAggregate> hasBestUri = c -> c.getAllUris()
//                                                              .stream()
//                                                              .anyMatch( allBestUris::contains );
//         final Predicate<Collection<ConceptAggregate>> setHasBestUri = c -> c.stream()
//                                                                             .anyMatch( hasBestUri );
//         final Collection<Collection<ConceptAggregate>> relations =
//               featureHelper.getNeoplasm()
//                            .getRelatedConceptMap()
//                             .entrySet()
//                             .stream()
//                             .filter( e -> RelationConstants.isHasSiteRelation( e.getKey() ) )
//                             .map( Map.Entry::getValue )
//                            .collect( Collectors.toList() );
//         _bestRelationCount = (int)relations.stream()
//                                            .filter( setHasBestUri )
//                                            .count();
//         _allSiteRelationCount = relations.size();
//         _patientRelationCount = (int)featureHelper.getAllPatientConcepts()
//                                                            .stream()
//                                                          .map( ConceptAggregate::getRelatedConceptMap )
//                                                          .map( Map::entrySet )
//                                                          .flatMap( Collection::stream )
//                                                          .filter( e -> RelationConstants.isHasSiteRelation( e.getKey() ) )
//                                                          .map( Map.Entry::getValue )
//                                                          .count();
//         //5.  !!!!!  Runner-Up  !!!!!
//         //    ======  URI  =====
//         //-
//         //    ======  CONCEPT  =====
//         //35. # runner-up concepts with site uri in neoplasm / # winner site concepts in neoplasm  (first order v first order)
//         //36. # runner-up concepts with site uri in neoplasm / # winner site concepts in neoplasm  (first order v any order)
//         //??. # runner-up concepts with site uri in neoplasm / # winner site concepts in neoplasm  (any order v any order)  - Could be a different runner-up, could be > 1
//         //37. # runner-up concepts with site uri in neoplasm / # winner site concepts in patient  (first order v patient)
//         //??. # runner-up concepts with site uri in neoplasm / # winner site concepts in patient  (any order v patient)  - Could be a different runner-up, could be > 1
//         //??. # runner-up concepts with site uri in patient / # winner site concepts in patient  (patient v patient)  - Could be a different runner-up, could be > 1
//         _runnerUpFirstConceptCount = _firstConceptBranchCounts.entrySet()
//                                                               .stream()
//                                                               .filter( e -> !_bestFirstConceptBranchCounts.containsKey( e.getKey() ) )
//                                                               .mapToInt( Map.Entry::getValue )
//                                                               .max()
//                                                               .orElse( 0 );
//         _runnerUpNeoplasmConceptCount = _neoplasmConceptBranchCounts.entrySet()
//                                                                     .stream()
//                                                                     .filter( e -> !_bestFirstConceptBranchCounts.containsKey( e.getKey() ) )
//                                                                     .mapToInt( Map.Entry::getValue )
//                                                                     .max()
//                                                                     .orElse( 0 );
//         _runnerUpPatientConceptCount = _patientConceptBranchCounts.entrySet()
//                                                                   .stream()
//                                                                   .filter( e -> !_bestFirstConceptBranchCounts.containsKey( e.getKey() ) )
//                                                                   .mapToInt( Map.Entry::getValue )
//                                                                   .max()
//                                                                   .orElse( 0 );
//         //    ======  MENTION  =====
//         //38. # runner-up mentions with site uri in neoplasm / # winner site mentions in neoplasm  (first order v first order)
//         //39. # runner-up mentions with site uri in neoplasm / # winner site mentions in neoplasm  (first order v any order)
//         //??. # runner-up mentions with site uri in neoplasm / # winner site mentions in neoplasm  (any order v any order)  - Could be a different runner-up, could be > 1
//         //40. # runner-up mentions with site uri in neoplasm / # winner site mentions in patient  (first order v patient)
//         //??. # runner-up mentions with site uri in neoplasm / # winner site mentions in patient  (any order v patient)  - Could be a different runner-up, could be > 1
//         //??. # runner-up mentions with site uri in patient / # winner site mentions in patient  (patient v patient)  - Could be a different runner-up, could be > 1
//         _runnerUpFirstMentionCount = _firstMentionBranchCounts.entrySet()
//                                                               .stream()
//                                                               .filter( e -> !_bestFirstMentionBranchCounts.containsKey( e.getKey() ) )
//                                                               .mapToInt( Map.Entry::getValue )
//                                                               .max()
//                                                               .orElse( 0 );
//         _runnerUpNeoplasmMentionCount = _neoplasmMentionBranchCounts.entrySet()
//                                                                     .stream()
//                                                                     .filter( e -> !_bestFirstMentionBranchCounts.containsKey( e.getKey() ) )
//                                                                     .mapToInt( Map.Entry::getValue )
//                                                                     .max()
//                                                                     .orElse( 0 );
//         _runnerUpPatientMentionCount = _patientMentionBranchCounts.entrySet()
//                                                                   .stream()
//                                                                   .filter( e -> !_bestFirstMentionBranchCounts.containsKey( e.getKey() ) )
//                                                                   .mapToInt( Map.Entry::getValue )
//                                                                   .max()
//                                                                   .orElse( 0 );
//      }
//   }
//
//
//
//
//
//
////      final Map<String,List<ConceptAggregate>> firstUriConceptMap =
////            siteStore.getFirstSites().stream().collect( Collectors.groupingBy( ConceptAggregate::getUri ) );
////      final List<String> otherFirstUris
////            = new ArrayList<>( firstUriConceptMap.keySet()
////                                                 .stream()
////                                                 .sorted( Comparator.comparingInt( u -> firstUriConceptMap.get( u ).size() ) )
////                                                 .collect( Collectors.toList() ) );
////      otherFirstUris.remove( siteStore._bestSiteUri );
////      final int otherFirstUriCount = otherFirstUris.isEmpty()
////                                     ? 0
////                                     : firstUriConceptMap.get(
////                                           otherFirstUris.get( otherFirstUris.size()-1 ) ).size();
////      final int feature35 = Math.min( 10, createFeature10( otherFirstUriCount,
////                                             firstUriConceptMap.get( siteStore._bestSiteUri ).size() ) );
////      final Map<String,List<ConceptAggregate>> neoplasmUriConceptMap =
////            siteStore.getNeoplasmSites().stream().collect( Collectors.groupingBy( ConceptAggregate::getUri ) );
////      final int feature36 = Math.min( 10, createFeature10( otherFirstUriCount,
////                                             neoplasmUriConceptMap.get( siteStore._bestSiteUri ).size() ) );
////      final List<String> otherNeoplasmUris
////            = new ArrayList<>( neoplasmUriConceptMap.keySet()
////                                                 .stream()
////                                                 .sorted( Comparator.comparingInt( u -> neoplasmUriConceptMap.get( u ).size() ) )
////                                                 .collect( Collectors.toList() ) );
////      otherNeoplasmUris.remove( siteStore._bestSiteUri );
////      final int otherNeoplasmUriCount = otherNeoplasmUris.isEmpty()
////                                     ? 0
////                                     : neoplasmUriConceptMap.get(
////                                           otherNeoplasmUris.get( otherNeoplasmUris.size()-1 ) ).size();
////      final int feature36_1 = Math.min( 10, createFeature10( otherNeoplasmUriCount,
////                                                           neoplasmUriConceptMap.get( siteStore._bestSiteUri ).size() ) );
////      final Map<String,List<ConceptAggregate>> patientUriConceptMap =
////            siteStore.getPatientSites().stream().collect( Collectors.groupingBy( ConceptAggregate::getUri ) );
////      final int feature37 = Math.min( 10, createFeature10( otherFirstUriCount,
////                                                           patientUriConceptMap.get( siteStore._bestSiteUri ).size() ) );
////
////      final int feature37_1 = Math.min( 10, createFeature10( otherNeoplasmUriCount,
////                                                             patientUriConceptMap.get( siteStore._bestSiteUri ).size() ) );
////      final List<String> otherPatientUris
////            = new ArrayList<>( patientUriConceptMap.keySet()
////                                                   .stream()
////                                                   .sorted( Comparator.comparingInt( u -> patientUriConceptMap.get( u ).size() ) )
////                                                   .collect( Collectors.toList() ) );
////      otherPatientUris.remove( siteStore._bestSiteUri );
////      final int otherPatientUriCount = otherPatientUris.isEmpty()
////                                       ? 0
////                                       : patientUriConceptMap.get(
////                                             otherPatientUris.get( otherPatientUris.size()-1 ) ).size();
////      final int feature37_2 = Math.min( 10, createFeature10( otherPatientUriCount,
////                                                             patientUriConceptMap.get( siteStore._bestSiteUri ).size() ) );
////
//
//
//   private List<Integer> createEmptyFeatures() {
//      final Integer[] features = new Integer[ 55 ];
//      Arrays.fill( features, -1 );
//      return Arrays.asList( features );
//   }
//
//}
