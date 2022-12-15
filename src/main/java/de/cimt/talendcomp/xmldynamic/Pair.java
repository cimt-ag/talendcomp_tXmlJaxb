/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.cimt.talendcomp.xmldynamic;

import java.io.Serializable;

/**
 * Simple typed pair implementation.
 * <p>
 * Note regarding serializing: if both X and Y are serializable types, the
 * pair itself will be serializable, too. If either type is not serializable,
 * the attempt to serialize such a pair will result in a NotSerializableException.
 * Much less of a hassle than working with two pair types combining serializable
 * and non-serializable bounded types.
 * <p>
 * This file is part of CollLib released under the terms of the LGPL V3.0.
 * See the file licenses/lgpl-3.0.txt for details.
 * 
 * @author marc.jackisch
 * @param <X>
 * @param <Y>
 */
public class Pair<X, Y> implements Serializable {
   public X x;
   public Y y;
   
   public Pair(X x, Y y) {
       this.x = x;
       this.y = y;
   }
   
   public Pair() {
      this(null, null);
   }
   
   public X getX() {
      return x;
   }
   
   public void setX(X x) {
      this.x = x;
   }
   
   public Y getY() {
      return y;
   }
   
   public void setY(Y y) {
      this.y = y;
   }

   @Override
   public int hashCode() {
       return (x != null ? x.hashCode() : 0)
            + (y != null ? y.hashCode() : 0);
   }
   
   @Override
   public boolean equals(Object o) {
       if(o instanceof Pair<?, ?>) {
          Pair<?, ?> other = (Pair<?, ?>) o;
          return nulleq(x, other.x) && nulleq(y, other.y);
       }
       return false;
   }
   
   private static boolean nulleq(Object a, Object b) {
      return (a != null) ? ((b != null) ? a.equals(b) : false) : b == null;
   }

   @Override
   public String toString() {
       return "Pair(" + x + ", " + y + ")";
   }
}
