package org.healthnlp.deepphe.summary.attribute.grade;

import org.healthnlp.deepphe.summary.attribute.infostore.UriInfoStore;
import org.healthnlp.deepphe.summary.concept.ConceptAggregate;


public class GradeCodeInfoStore {

   public String _bestGradeCode;


   protected void init( final UriInfoStore uriInfoStore ) {
      _bestGradeCode = getBestGradeCode( uriInfoStore._bestUri );
   }

   static private String getBestGradeCode( final String bestUri ) {
      final int gradeNumber = getUriGradeNumber( bestUri );
      if ( gradeNumber < 0 ) {
         return "9";
      }
      return "" + gradeNumber;
   }


   static public int getGradeNumber( final ConceptAggregate grade ) {
      return getUriGradeNumber( grade.getUri() );
   }

   static public int getUriGradeNumber( final String uri ) {
      if ( uri.startsWith( "Gleason_Score_" ) ) {
         if ( uri.endsWith( "6" ) ) {
            return 1;
         } else if ( uri.endsWith( "7" ) ) {
            return 2;
         } else if ( uri.endsWith( "8" )
                     || uri.endsWith( "9" )
                     || uri.endsWith( "10" ) ) {
            return 3;
         } else {
            return -1;
         }
      } else if ( uri.equals( "Grade_1" )
                  || uri.equals( "Low_Grade" )
                  || uri.equals( "Low_Grade_Malignant_Neoplasm" )
                  || uri.equals( "Well_Differentiated" ) ) {
         return 1;
      } else if ( uri.equals( "Grade_2" )
                  || uri.equals( "Intermediate_Grade" )
                  || uri.equals( "Intermediate_Grade_Malignant_Neoplasm" )
                  || uri.equals( "Moderately_Differentiated" ) ) {
         return 2;
      } else if ( uri.equals( "Grade_3" )
                  || uri.equals( "High_Grade" )
                  || uri.equals( "High_Grade_Malignant_Neoplasm" )
                  || uri.equals( "Poorly_Differentiated" ) ) {
         return 3;
      } else if ( uri.equals( "Grade_4" )
                  || uri.equals( "Undifferentiated" ) ) {
         // todo add "anaplastic"
//            LOGGER.info( "Have an Undifferentiated, adding its Grade Equivalent (4) to possible ICDO Grades." );
         return 4;
      } else if ( uri.equals( "Grade_5" ) ) {
         return 5;
      }
      return -1;
   }

}
