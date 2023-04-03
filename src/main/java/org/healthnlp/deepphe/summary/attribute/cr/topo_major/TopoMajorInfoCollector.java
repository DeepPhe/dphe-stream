package org.healthnlp.deepphe.summary.attribute.cr.topo_major;

import org.healthnlp.deepphe.neo4j.constant.RelationConstants;
import org.healthnlp.deepphe.neo4j.node.Mention;
import org.healthnlp.deepphe.neo4j.node.Note;
import org.healthnlp.deepphe.summary.attribute.cr.newInfoStore.AbstractAttributeInfoCollector;

import java.util.Collection;
import java.util.Collections;

/**
 * @author SPF , chip-nlp
 * @since {3/25/2023}
 */
public class TopoMajorInfoCollector extends AbstractAttributeInfoCollector {

   static private final String UNKNOWN_PRIMARY_URI = "Whole_Body";

   static private final int SITE_LEFT_WINDOW = 25;
   static private final int SITE_RIGHT_WINDOW = 10;


   public Collection<String> getRelationTypes() {
      return Collections.singletonList( RelationConstants.DISEASE_HAS_ASSOCIATED_ANATOMIC_SITE );
   }


   // TODO Move this to Relation Creator and offset confidence by relation type.
   //  Instead of using character distance use token count.

   static private boolean hasExactPreText( final Note note, final Mention mention ) {
      final int mentionBegin = mention.getBegin();
      if ( mentionBegin <= SITE_LEFT_WINDOW ) {
         return false;
      }
      final String preText = note.getText()
                                 .substring( mentionBegin - SITE_LEFT_WINDOW, mentionBegin )
                                 .toLowerCase();
//      NeoplasmSummaryCreator.addDebug( "Topography exact Candidate and pretext "
//                                              + note.getText()
//                                                    .substring( mentionBegin - SITE_LEFT_WINDOW,
//                                                                mention.getEnd() )
//                                              + "\n" );
      return preText.contains( "tumor site:" );
   }

   static private boolean hasSupportPreText( final Note note, final Mention mention ) {
      final int mentionBegin = mention.getBegin();
      if ( mentionBegin <= SITE_LEFT_WINDOW ) {
         return false;
      }
      final String preText = note.getText()
                                 .substring( mentionBegin - SITE_LEFT_WINDOW, mentionBegin )
                                 .toLowerCase();
//      NeoplasmSummaryCreator.addDebug( "Topography support Candidate and pretext "
//                                              + note.getText()
//                                                    .substring( mentionBegin - SITE_LEFT_WINDOW,
//                                                                mention.getEnd() )
//                                              + "\n" );
      return preText.contains( "supportive of" )
             || preText.contains( "support possible" )
             || preText.contains( "probable" );
   }

   static private boolean hasOriginPostText( final Note note, final Mention mention ) {
      final int mentionEnd = mention.getEnd();
      final String noteText = note.getText();
      if ( mentionEnd + SITE_RIGHT_WINDOW > noteText.length() ) {
         return false;
      }
      final String postText = noteText
            .substring( mentionEnd, mentionEnd + SITE_RIGHT_WINDOW )
            .toLowerCase();
//      NeoplasmSummaryCreator.addDebug( "Topography origin Candidate and postext "
//                                              + note.getText()
//                                                    .substring( mention.getBegin(), mentionEnd + SITE_RIGHT_WINDOW )
//                                              + "\n" );
      return postText.contains( "origin" ) || postText.contains( "primary" );
   }

}
