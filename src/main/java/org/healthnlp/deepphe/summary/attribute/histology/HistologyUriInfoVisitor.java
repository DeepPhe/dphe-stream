package org.healthnlp.deepphe.summary.attribute.histology;

import org.healthnlp.deepphe.neo4j.constant.UriConstants;
import org.healthnlp.deepphe.neo4j.embedded.EmbeddedConnection;
import org.healthnlp.deepphe.neo4j.node.Mention;
import org.healthnlp.deepphe.neo4j.node.Note;
import org.healthnlp.deepphe.node.NoteNodeStore;
import org.healthnlp.deepphe.summary.attribute.infostore.UriInfoVisitor;
import org.healthnlp.deepphe.summary.concept.ConceptAggregate;
import org.healthnlp.deepphe.summary.engine.NeoplasmSummaryCreator;
import org.neo4j.graphdb.GraphDatabaseService;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.stream.Collectors;

import static org.healthnlp.deepphe.neo4j.constant.RelationConstants.HAS_DIAGNOSIS;
import static org.healthnlp.deepphe.neo4j.constant.RelationConstants.HAS_TUMOR_EXTENT;

final public class HistologyUriInfoVisitor implements UriInfoVisitor {

   private Collection<ConceptAggregate> _histologyConcepts;
   private Collection<String> _exactHistologyUris = new HashSet<>();

   static private final int HISTOLOGY_WINDOW = 25;
   @Override
   public Collection<ConceptAggregate> getAttributeConcepts( final Collection<ConceptAggregate> neoplasms ) {
      if ( _histologyConcepts == null ) {
         _exactHistologyUris.clear();
         final GraphDatabaseService graphDb = EmbeddedConnection.getInstance()
                                                                .getGraph();
         final Collection<String> neoplasmUris = UriConstants.getNeoplasmUris( graphDb );
         final Collection<ConceptAggregate> certainNeoplasm = neoplasms.stream()
//                                                                         .filter( c -> !c.isNegated() )
                                                                         .filter( c -> c.getAllUris()
                                                                                        .stream()
                                                                                        .anyMatch( neoplasmUris::contains ) )
                                                                         .collect( Collectors.toSet() );
         _histologyConcepts = new HashSet<>( certainNeoplasm );
         neoplasms.stream()
                    .map( c -> c.getRelated( HAS_DIAGNOSIS, HAS_TUMOR_EXTENT ) )
                    .flatMap( Collection::stream )
//                    .filter( c -> !c.isNegated() )
//                   .filter( c -> !c.isUncertain() )
                   .forEach( _histologyConcepts::add );
//         if ( !_histologyConcepts.isEmpty() ) {
//            return _histologyConcepts;
//         }
//         neoplasms.stream()
//                 .filter( c -> !c.isNegated() )
//                  .filter( c -> c.getAllUris()
//                                 .stream()
//                                 .anyMatch( neoplasmUris::contains ) )
//                  .forEach( _histologyConcepts::add );
//         _histologyConcepts = new HashSet<>( certainNeoplasm );
//         neoplasms.stream()
//                  .map( c -> c.getRelated( HAS_DIAGNOSIS, HAS_TUMOR_EXTENT ) )
//                  .flatMap( Collection::stream )
//                  .filter( c -> !c.isNegated() )
//                  .forEach( _histologyConcepts::add );


         //  Added 3/31/2022
         //  If text contains "histologic type: [type]" for any detected aggregates only those are returned.
         final Collection<ConceptAggregate> histologies = new HashSet<>();
         for ( ConceptAggregate aggregate : _histologyConcepts ) {
            for ( Mention mention : aggregate.getMentions() ) {
               final int mentionBegin = mention.getBegin();
               if ( mentionBegin <= HISTOLOGY_WINDOW ) {
                  continue;
               }
               final Note note = NoteNodeStore.getInstance().get( mention.getNoteId() );
               if ( note == null ) {
//                  LOGGER.warn( "No Note stored for Note ID " + mention.getNoteId() );
                  continue;
               }
               final String preText = note.getText()
                                          .substring( mentionBegin-HISTOLOGY_WINDOW, mentionBegin )
                                          .toLowerCase();
               NeoplasmSummaryCreator.DEBUG_SB.append( "Histology Candidate and pretext "
                                                       + note.getText().substring( mentionBegin-HISTOLOGY_WINDOW, mention.getEnd() )
                                                       + "\n" );
               if ( preText.contains( "histologic type:" ) || preText.contains( "diagnosis:" ) ) {
                  NeoplasmSummaryCreator.DEBUG_SB.append( "Trimming to histology candidate "
                                                          + aggregate.getCoveredText() + "\n" );
                  histologies.add( aggregate );
                  _exactHistologyUris.add( mention.getClassUri() );
               }
            }
         }
         if ( !histologies.isEmpty() ) {
            _histologyConcepts.retainAll( histologies );
         }
      }
      return _histologyConcepts;
   }


   /**
    * Grade uris are ranked in order of represented grade number regardless of the uri quotients.
    * @param neoplasms -
    * @return the histology score as it may be increased by text surrounding a mention.
    */
   @Override
   public Map<String,Integer> getAttributeUriStrengths( final Collection<ConceptAggregate> neoplasms ) {
      final Map<String,Integer> uriStrengths = UriInfoVisitor.super.getAttributeUriStrengths( neoplasms );
      if ( _exactHistologyUris.isEmpty() ) {
         return uriStrengths;
      }
      for ( String uri : _exactHistologyUris ) {
         final int strength = uriStrengths.get( uri );
         NeoplasmSummaryCreator.DEBUG_SB.append( "Adding 10% strength to Histology Candidate " + uri
                                                 + " strength " + strength + "\n" );
         uriStrengths.put( uri, strength + 10 );
      }
      return uriStrengths;
   }



//   @Override
//   public boolean applySectionStrengths() {
//      return true;
//   }

//   /**
//    * Grade uris are ranked in order of represented grade number regardless of the uri quotients.
//    * @param neoplasms -
//    * @return the grade score (0-5) * 20.
//    */
//   @Override
//   public Map<String,Integer> getAttributeUriStrengths( final Collection<ConceptAggregate> neoplasms ) {
//      final Collection<ConceptAggregate> concepts = getAttributeConcepts( neoplasms );
//      return concepts.stream()
//                     .map( ConceptAggregate::getAllUris )
//                     .flatMap( Collection::stream )
//                     .distinct()
//                     .collect( Collectors.toMap( Function.identity(),
//                                                 HistologyUriInfoVisitor::getHistologyStrength ) );
//   }
//
//   static private int getHistologyStrength( final String uri ) {
//      return Math.max( 0, Math.min( 100, GradeCodeInfoStore.getUriGradeNumber( uri ) * 20 ) );
//   }

}
