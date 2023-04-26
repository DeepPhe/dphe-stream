package org.healthnlp.deepphe.summary.attribute.cr.grade;

import org.healthnlp.deepphe.neo4j.constant.RelationConstants;
import org.healthnlp.deepphe.summary.attribute.cr.newInfoStore.AbstractAttributeInfoCollector;

import java.util.Arrays;
import java.util.Collection;


/**
 * @author SPF , chip-nlp
 * @since {3/23/2023}
 */
public class GradeInfoCollector extends AbstractAttributeInfoCollector {

//   static private final int GRADE_WINDOW = 25;

   public Collection<String> getRelationTypes() {
      return Arrays.asList( RelationConstants.HAS_GRADE, RelationConstants.HAS_GLEASON_SCORE );
   }

   // Is this necessary?
//   public Collection<CrConceptAggregate> getBestAggregates() {
//      final Collection<CrConceptAggregate> aggregates = getAllAggregates();
//      if ( aggregates.isEmpty() ) {
//         return Collections.emptyList();
//      }
//      final ConfidenceGroup<CrConceptAggregate> group = new ConfidenceGroup<>( aggregates );
//      final Collection<CrConceptAggregate> grades = getNonNuclearGrades( group.getBest() );
//      if ( !grades.isEmpty() ) {
//         return grades;
//      }
//      grades.addAll( getNonNuclearGrades( group.getNext() ) );
//      if ( !grades.isEmpty() ) {
//         return grades;
//      }
//      return getNonNuclearGrades( group.getOther() );
//   }

//   static private Collection<CrConceptAggregate> getNonNuclearGrades( final Collection<CrConceptAggregate> aggregates ) {
//      final Collection<CrConceptAggregate> nuclearGrades = new HashSet<>();
//      for ( CrConceptAggregate aggregate : aggregates ) {
//         int nuclearCount = 0;
//         for ( Mention mention : aggregate.getMentions() ) {
//            final int mentionBegin = mention.getBegin();
//            if ( mentionBegin <= GRADE_WINDOW ) {
//               continue;
//            }
//            final Note note = NoteNodeStore.getInstance()
//                                           .get( mention.getNoteId() );
//            if ( note == null ) {
////                  LOGGER.warn( "No Note stored for Note ID " + mention.getNoteId() );
//               continue;
//            }
//            final String preText = note.getText()
//                                       .substring( mentionBegin - GRADE_WINDOW, mentionBegin )
//                                       .toLowerCase();
//            if ( preText.contains( "nuclear" ) ) {
//               nuclearCount++;
//            }
//         }
//         if ( nuclearCount == aggregate.getMentions().size() ) {
//            nuclearGrades.add( aggregate );
//         }
//      }
//      aggregates.removeAll( nuclearGrades );
//      return aggregates;
//   }


}
