package org.healthnlp.deepphe.summary.engine;

import org.apache.ctakes.core.util.annotation.SemanticTui;
import org.healthnlp.deepphe.neo4j.node.*;
import org.healthnlp.deepphe.nlp.uri.UriInfoCache;
import org.healthnlp.deepphe.summary.concept.ConfidenceGroup;
import org.healthnlp.deepphe.summary.concept.CrConceptAggregate;
import org.healthnlp.deepphe.summary.concept.CrConceptAggregateCreator;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author SPF , chip-nlp
 * @since {3/12/2023}
 */
final public class CrSummaryEngine {

   private CrSummaryEngine() {}


   static public PatientSummary createPatientSummary( final Patient patient ) {
      final String patientId = patient.getId();
      final Map<Mention, String> patientMentionNoteIds = new HashMap<>();
      final Collection<MentionRelation> patientRelations = new ArrayList<>();
      final Collection<Note> patientNotes = patient.getNotes();
      for ( Note note : patientNotes ) {
         final String noteId = note.getId();
         note.getMentions().forEach( m -> patientMentionNoteIds.put( m, noteId ) );
         patientRelations.addAll( note.getRelations() );


//         LOGGER.info( "\n====================== Note " + noteId + " ======================" );
//         LOGGER.info( "We have completed the per-document nlp and trimmed it to information essential for summarization." +
//                      "  Below are the mentions, relations between mentions, and mention coreference chains for the document " + noteId + "." );
//         note.getMentions().forEach( m -> LOGGER.info( "Mention: " + m.getClassUri() + " " + m.getId() ) );
//         note.getRelations().forEach( r -> LOGGER.info( "Relation: " + r.getSourceId() + " " + r.getType() + " " + r.getTargetId() ) );
//         LOGGER.info( "Within doc corefs are no longer being dicovered by NLP pipeline." );
//         note.getCorefs().forEach( c -> LOGGER.info( "Chain: (" + String.join( ",", Arrays.asList( c.getIdChain() ) ) + ")" ) );
      }

      final PatientSummary patientSummary = createCrPatientSummary( patientId,
                                                                  patientMentionNoteIds,
                                                                  patientRelations );
      patientSummary.setPatient( patient );
      return patientSummary;
   }


   /**
    * Entry point for new multi-cancer, drools-free summary creation.
    *
    * @param patientId   -
    * @param patientMentionNoteIds -
    * @param patientRelations   -
    * @return map of cancer summary to tumor summaries
    */
   static private PatientSummary createCrPatientSummary( final String patientId,
                                                       final Map<Mention, String> patientMentionNoteIds,
                                                       final Collection<MentionRelation> patientRelations ) {
//      LOGGER.info( "\n====================== Creating Concept Aggregates for " + patientId + " ======================" );
//      LOGGER.info( "Concept Aggregates are basically unique concepts that are created by aggregating all mentions that are correferent." +
//                   "  While coreference chains are within single documents, Concept Aggregates span across all documents." );
//      +
//                   "  Concept Aggregates do not only aggregate cross-document mentions, but will also both aggregate and separate" +
//                   " mentions in within-document coreference chains." +
//                   "  So, yes, we could logically remove the coreference annotator from the nlp pipeline." +
//                   "   I will experiment when I have time.   - 10/14/2020 Done." );
//      LOGGER.info( "For the patient we have " + patientNotes.size() + " notes, "
//                   + patientMentionNoteIds.size() + " mentions, "
//                   + patientRelations.size() + " relations" );
//                   + patientCorefs.size() + " coref chains." );
NeoplasmSummaryCreator.addDebug( "CreatePatientSummary: " + patientId + "\n" );
      final Map<String,Collection<CrConceptAggregate>> uriCrAggregateMap
            = CrConceptAggregateCreator.createUriConceptAggregateMap( patientId,
                                                                      patientMentionNoteIds,
                                                                      patientRelations );
//      final Collection<ConceptAggregate> allConcepts
//            = uriCrAggregateMap.values().stream().flatMap( Collection::stream ).collect( Collectors.toSet() );

      final Collection<CrConceptAggregate> neoplasms
            = uriCrAggregateMap.entrySet()
                               .stream()
                               .filter( e -> UriInfoCache.getInstance().getSemanticTui( e.getKey() ) == SemanticTui.T191 )
                               .map( Map.Entry::getValue )
                               .flatMap( Collection::stream )
                               .collect( Collectors.toSet() );

      Collection<CrConceptAggregate> summaryNeoplasms = neoplasms.stream()
                                                                   .filter( CrSummaryEngine::mostlyAffirmed )
                                                                   .collect( Collectors.toList() );
      if ( summaryNeoplasms.isEmpty() ) {
         // No affirmed neoplasms, go with half affirmed.
         summaryNeoplasms = neoplasms.stream()
                                     .filter( CrSummaryEngine::halfAffirmed )
                                     .collect( Collectors.toList() );
      }
      if ( summaryNeoplasms.isEmpty() ) {
         // No affirmed neoplasms, go with negated.
         summaryNeoplasms = neoplasms;
      }

      NeoplasmSummaryCreator.addDebug( "CreatePatientSummary: " + patientId + " "
                                       + summaryNeoplasms.stream().map( n -> n.getUri() + ":" + n.getConfidence() )
                                                  .collect( Collectors.joining(" , " ) ) + "\n" );

//      final Collection<CrConceptAggregate> topmostNeoplasms = new ConfidenceGroup<>( neoplasms ).getTopmost();
      final Collection<CrConceptAggregate> topmostNeoplasms = new ConfidenceGroup<>( summaryNeoplasms ).getBest();
      final List<NeoplasmSummary> topmostNeoplasmSummaries
            = topmostNeoplasms.stream()
                              .map( CrNeoplasmSummaryCreator::createCrNeoplasmSummary )
                              .collect( Collectors.toList() );

      final PatientSummary patientSummary = new PatientSummary();
      patientSummary.setId( patientId );
      patientSummary.setNeoplasms( topmostNeoplasmSummaries );
      return patientSummary;
   }

   static private boolean mostlyAffirmed( final CrConceptAggregate aggregate ) {
      final Collection<Mention> mentions = aggregate.getMentions();
      final long negated = mentions.stream().filter( Mention::isNegated ).count();
      return (double)negated / mentions.size() <= 0.25;
   }

   static private boolean halfAffirmed( final CrConceptAggregate aggregate ) {
      final Collection<Mention> mentions = aggregate.getMentions();
      final long negated = mentions.stream().filter( Mention::isNegated ).count();
      return (double)negated / mentions.size() <= 0.5;
   }


}