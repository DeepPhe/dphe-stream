package org.healthnlp.deepphe.node;

import org.apache.ctakes.core.store.ObjectCreator;
import org.healthnlp.deepphe.neo4j.node.Note;
import org.healthnlp.deepphe.neo4j.node.Patient;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public final class PatientCreator implements ObjectCreator<Patient> {

   public Patient create( final String patientId ) {
      final Patient patient = new Patient();
      patient.setId( patientId );
      patient.setBirth( "" );
      patient.setDeath( "" );
      patient.setGender( "" );
      patient.setName( "" );
      patient.setNotes( new ArrayList<>() );
      return patient;
   }

   static public void addNote( final Patient patient, final Note note ) {
      final String noteId = note.getId();
      final List<Note> notes = patient.getNotes();
      final Collection<Note> removalNotes
            = notes.stream()
                   .filter( n -> n.getId().equals( noteId ) )
                   .collect( Collectors.toSet() );
      notes.removeAll( removalNotes );
      notes.add( note );
      patient.setNotes( notes );
   }

}
