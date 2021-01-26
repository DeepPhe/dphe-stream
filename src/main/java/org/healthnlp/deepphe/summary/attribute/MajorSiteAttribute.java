package org.healthnlp.deepphe.summary.attribute;

import org.healthnlp.deepphe.neo4j.constant.UriConstants;
import org.healthnlp.deepphe.neo4j.embedded.EmbeddedConnection;
import org.healthnlp.deepphe.neo4j.node.Mention;
import org.healthnlp.deepphe.neo4j.node.NeoplasmAttribute;
import org.healthnlp.deepphe.summary.concept.ConceptAggregate;
import org.neo4j.graphdb.GraphDatabaseService;

import java.util.*;
import java.util.stream.Collectors;

import static org.healthnlp.deepphe.summary.attribute.SpecificAttribute.EvidenceLevel.*;

@Deprecated
public class MajorSiteAttribute implements SpecificAttribute {


   final private NeoplasmAttribute _neoplasmAttribute;


   public MajorSiteAttribute( final ConceptAggregate neoplasm,
                           final Collection<ConceptAggregate> allConcepts ) {
      _neoplasmAttribute = createMajorSiteAttribute( neoplasm, allConcepts );
   }

   public NeoplasmAttribute toNeoplasmAttribute() {
      return _neoplasmAttribute;
   }

   private NeoplasmAttribute createMajorSiteAttribute( final ConceptAggregate neoplasm,
                                                       final Collection<ConceptAggregate> allConcepts ) {
      final SiteAttributeHelper helper = new SiteAttributeHelper( neoplasm, allConcepts );

      final String bestUri = helper.getBestUriForType();
      if ( bestUri == null || bestUri.isEmpty() || bestUri.equals( "DeepPhe" ) ) {
         return SpecificAttribute.createAttributeWithFeatures( "topography_major",
                                                   "C80",
                                                   Collections.emptyList(),
                                                   Collections.emptyList(),
                                                   helper.createEmptyFeatures() );
      }
      final Collection<ConceptAggregate> allPatientSites = collectPatientSites( allConcepts );
      final List<Integer> features = helper.createFeatures();
      final Map<EvidenceLevel,Collection<Mention>> evidence
            = SpecificAttribute.mapEvidence( helper.getBestConceptsForType(),
                                             neoplasm.getRelatedSites(),
                                             allPatientSites );
      return SpecificAttribute.createAttributeWithFeatures( "topography_major",
                                                                  helper.getBestIcdoCode(),
                                                                  new ArrayList<>( evidence.get( DIRECT_EVIDENCE ) ),
                                                                  new ArrayList<>( evidence.get( INDIRECT_EVIDENCE ) ),
                                                                  new ArrayList<>( evidence.get( NOT_EVIDENCE ) ),
                                                                  features );
   }


   private Collection<ConceptAggregate> collectPatientSites( final Collection<ConceptAggregate> allConcepts ) {
      final GraphDatabaseService graphDb = EmbeddedConnection.getInstance()
                                                             .getGraph();
      final Collection<String> siteUris = UriConstants.getLocationUris( graphDb );
      return allConcepts.stream()
                        .filter( c -> siteUris.contains( c.getUri() ) )
                        .collect( Collectors.toSet() );
   }


}
