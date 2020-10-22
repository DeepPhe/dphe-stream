package org.healthnlp.deepphe.node;


import org.healthnlp.deepphe.neo4j.node.Patient;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 9/4/2020
 */
public enum PatientNodeStore implements NodeStore<Patient> {
   INSTANCE;

   public static PatientNodeStore getInstance() {
      return INSTANCE;
   }


   private final Map<String, Patient> _patients;

   PatientNodeStore() {
      _patients = new HashMap<>();
   }

   public Patient get( final String patientId ) {
      return _patients.get( patientId );
   }

   public boolean add( final Patient patient ) {
      final String patientId = patient.getId();
      if ( patientId == null ) {
         return false;
      }
      return add( patientId, patient );
   }

   public boolean add( final String patientId, final Patient patient ) {
      _patients.put( patientId, patient );
      return true;
   }

   public Patient create( final String id ) {
      final Patient patient = new Patient();
      patient.setId( id );
      patient.setNotes( Collections.emptyList() );
      return patient;
   }

}
