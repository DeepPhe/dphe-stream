package org.healthnlp.deepphe.summary.attribute.cr.topo_minor.crc;

import org.healthnlp.deepphe.summary.attribute.cr.newInfoStore.AbstractAttributeNormalizer;
import org.healthnlp.deepphe.summary.attribute.cr.newInfoStore.AttributeInfoCollector;

/**
 * @author SPF , chip-nlp
 * @since {3/23/2023}
 */
public class AnusNormalizer extends AbstractAttributeNormalizer {

   public String getBestCode( final AttributeInfoCollector infoCollector ) {
      return getBestIntCode( infoCollector.getAllRelations() );
   }

   public String getDefaultTextCode() {
      return "0";
   }

   public int getIntCode( final String uri ) {
      if ( CrcUriCollection.getInstance().getAnalCanalUris().contains( uri ) ) {
         return 1;
      }
      if ( CrcUriCollection.getInstance().getCloacogenicZone().equals( uri ) ) {
         return 2;
      }
      if ( CrcUriCollection.getInstance().getAnorectalUri().equals( uri ) ) {
         return 8;
      }
      if ( CrcUriCollection.getInstance().getAnusUris().contains( uri ) ) {
         return 0;
      }
      return -1;
   }

   //Anus and Anal canal
//C21.0	Anus, NOS (excludes Skin of anus and Perianal skin C44.5)
//C21.1	Anal canal
//C21.2	Cloacogenic zone
//C21.8	Overlapping lesion of rectum, anus and anal canal


}
