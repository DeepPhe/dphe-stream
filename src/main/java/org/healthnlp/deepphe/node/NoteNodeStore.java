package org.healthnlp.deepphe.node;


import org.healthnlp.deepphe.neo4j.node.Note;

import java.util.HashMap;
import java.util.Map;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 9/4/2020
 */
public enum NoteNodeStore implements NodeStore<Note> {
   INSTANCE;

   public static NoteNodeStore getInstance() {
      return INSTANCE;
   }


   private final Map<String, Note> _notes;

   NoteNodeStore() {
      _notes = new HashMap<>();
   }

   public Note get( final String noteId ) {
      return _notes.get( noteId );
   }

   public boolean add( final Note note ) {
      final String noteId = note.getId();
      if ( noteId == null ) {
         return false;
      }
      return add( noteId, note );
   }

   public boolean add( final String noteId, final Note note ) {
      _notes.put( noteId, note );
      return true;
   }

}
