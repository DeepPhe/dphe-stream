package org.healthnlp.deepphe.node;

/**
 * @author SPF , chip-nlp
 * @version %I%
 * @since 9/17/2020
 */
public interface NodeStore<T> {

   T get( String id );

   default T getOrCreate( String id ) {
      final T stored = get( id );
      if ( stored != null ) {
         return stored;
      }
      final T newObject = create( id );
      if ( newObject == null ) {
         return null;
      }
      add( id, newObject );
      return newObject;
   }

   boolean add( String id, T object );

   default boolean add( T object ) {
      return false;
   }

   default T create( String id ) {
      return null;
   }


}
