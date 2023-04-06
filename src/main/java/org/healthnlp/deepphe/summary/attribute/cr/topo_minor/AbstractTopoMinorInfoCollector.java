package org.healthnlp.deepphe.summary.attribute.cr.topo_minor;

import org.healthnlp.deepphe.neo4j.node.Mention;
import org.healthnlp.deepphe.neo4j.node.Note;
import org.healthnlp.deepphe.node.NoteNodeStore;
import org.healthnlp.deepphe.summary.attribute.cr.newInfoStore.AbstractAttributeInfoCollector;
import org.healthnlp.deepphe.summary.concept.CrConceptAggregate;
import org.healthnlp.deepphe.summary.engine.NeoplasmSummaryCreator;

import java.util.Collection;
import java.util.HashSet;

/**
 * @author SPF , chip-nlp
 * @since {3/26/2023}
 */
abstract public class AbstractTopoMinorInfoCollector extends AbstractAttributeInfoCollector {

   static private final int SITE_LEFT_WINDOW = 25;
   static private final int SITE_RIGHT_WINDOW = 10;

   // Is this necessary?
   public Collection<CrConceptAggregate> getBestAggregates() {
      final Collection<CrConceptAggregate> statedMinors = new HashSet<>();
//      final Collection<CrConceptAggregate> aggregates = super.getBestAggregates();
      final Collection<CrConceptAggregate> aggregates = getAllAggregates();
      for ( CrConceptAggregate aggregate : aggregates ) {
         for ( Mention mention : aggregate.getMentions() ) {
            if ( hasExactText( mention ) ) {
               statedMinors.add( aggregate );
               NeoplasmSummaryCreator.addDebug( "Trimming to minor candidate "
                                                + aggregate.getCoveredText() + "\n" );
               break;
            }
         }
      }
      if ( !statedMinors.isEmpty() ) {
         return statedMinors;
      }
      return aggregates;
   }


   private boolean hasExactText( final Mention mention ) {
      final Note note = NoteNodeStore.getInstance()
                                     .get( mention.getNoteId() );
      if ( note == null ) {
         return false;
      }
      return hasExactPreText( note, mention ) || hasExactPostText( note, mention );
   }

   static private boolean hasExactPreText( final Note note, final Mention mention ) {
      final int mentionBegin = mention.getBegin();
      if ( mentionBegin <= SITE_LEFT_WINDOW ) {
         return false;
      }
      final String preText = note.getText()
                                 .substring( mentionBegin - SITE_LEFT_WINDOW, mentionBegin )
                                 .toLowerCase();
      NeoplasmSummaryCreator.addDebug( "minor Candidate and pretext "
                                       + note.getText()
                                             .substring( mentionBegin - SITE_LEFT_WINDOW,
                                                         mention.getEnd() )
                                       + "\n" );
      return preText.contains( "tumor site:" ) || preText.contains( "supportive of" );
   }

   static private boolean hasExactPostText( final Note note, final Mention mention ) {
      final int mentionEnd = mention.getEnd();
      final String noteText = note.getText();
      if ( mentionEnd + SITE_RIGHT_WINDOW > noteText.length() ) {
         return false;
      }
      final String postText = noteText
            .substring( mentionEnd, mentionEnd + SITE_RIGHT_WINDOW )
            .toLowerCase();
      NeoplasmSummaryCreator.addDebug( "minor Candidate and postext "
                                       + note.getText()
                                             .substring( mentionEnd, mentionEnd + SITE_RIGHT_WINDOW )
                                       + "\n" );
      return postText.contains( "origin" ) || postText.contains( "primary" );
   }

}
