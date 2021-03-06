/*
 * Copyright(C) Triniforce
 * All Rights Reserved.
 *
 */ 
package com.triniforce.eval;

import java.util.Arrays;

import com.triniforce.db.test.TFTestCase;

public class OlEvalTest extends TFTestCase {

	
    public void testConstructor(){
        OlEval of = new OlEval();
        assertTrue(of.evalArray(new Object[]{}, 0));
    }
    
    
    public void testThreeValued(){
        OlEval ev = new OlEval();
        
        IOlEvaluator nullEv = new IOlEvaluator(){
            public Boolean evaluateThreeValued(IOlColumnGetter vg) {
                return null;
            }
        };
        
        IOlEvaluator trueEv = new IOlEvaluator(){
            public Boolean evaluateThreeValued(IOlColumnGetter vg) {
                return true;
            }
        };
        
        ev.addEval(trueEv);
        assertTrue(ev.evalArray(new Object[]{}, 0));
        ev.addEval(nullEv);
        assertFalse(ev.evalArray(new Object[]{}, 0));
        assertNull(ev.evaluateThreeValued(null));
        
    }
    
    public void testEvaluate(){
            
            // (val[0] == 1 AND val[1] == 2) OR (val[2] == 3 AND val[3] == 4)
            
            OlEval e0 = new OlEval();
            e0.setAndConcatenation(false);
            
            OlEval e1 = new OlEval();
            OlEval e2 = new OlEval();
            
            e1.addExpr(0, new OlBExprEquals(1));
            e1.addExpr(1, new OlBExprEquals(2));
            
            e2.addExpr(2, new OlBExprEquals(3));
            e2.addExpr(3, new OlBExprEquals(4));
            
            e0.addEval(e1);
            e0.addEval(e2);
            
            assertTrue(e0.evalArray(new Object[]{1, 2, 3, 4}, 0));
            assertTrue(e0.evalArray(new Object[]{1, 2, 3, 3}, 0));
            assertTrue(e0.evalArray(new Object[]{1, 2, 0, 0}, 0));
            assertTrue(e0.evalArray(new Object[]{0, 0, 3, 4}, 0));
            assertFalse(e0.evalArray(new Object[]{1, 1, 3, 3}, 0));
            assertFalse(e0.evalArray(new Object[]{2, 2, 4, 4}, 0));
        }

    public void testEquals(){
        
        OlEval ev = new OlEval();
        ev.addExpr(0, new OlBExprEquals(new OlExprColumn(2)));
        assertTrue(ev.evalArray(new Object[]{1, 1, 1}, 0));
        assertFalse(ev.evalArray(new Object[]{1, 1, 2}, 0));
    }
    
    public void testNot(){
    	
    	//not = 15678
    	{
    		OlEval ev = new OlEval();
    		ev.setNot(true);
    		ev.addExpr(0, new OlBExprEquals(15678));
    		assertTrue(ev.evalArray(new Object[]{6}, 0));
    		assertFalse(ev.evalArray(new Object[]{15678}, 0));
    	}
    	
    	//not ( in(1,4) or EQ(2) )
    	{
    		OlEval of = new OlEval();
    		of.setNot(true);
    		of.setAndConcatenation(false);
    		of.addExpr(0, new OlBExprIN(new Object[]{1, 4}));
    		of.addExpr(0, new OlBExprEquals(2));
    		assertTrue(of.evalArray(new Object[]{6}, 0));
    		assertTrue(of.evalArray(new Object[]{3}, 0));
    		assertFalse(of.evalArray(new Object[]{2}, 0));
    	}
    	
    }

    public void testOr(){
        OlEval of = new OlEval();
        of.setAndConcatenation(false);
        of.addExpr(0, new OlBExprEquals(1));
        of.addExpr(0, new OlBExprEquals(2));
        assertFalse(of.evalArray(new Object[]{6}, 0));
        assertFalse(of.evalArray(new Object[]{3}, 0));
        assertTrue(of.evalArray(new Object[]{1}, 0));
        assertTrue(of.evalArray(new Object[]{2}, 0));
        of.addExpr(1, new OlBExprEquals(3));
        of.addExpr(1, new OlBExprEquals(4));
        assertTrue(of.evalArray(new Object[]{2, 3}, 0));
        assertTrue(of.evalArray(new Object[]{2, 4}, 0));
        assertTrue(of.evalArray(new Object[]{6, 3}, 0));
        assertFalse(of.evalArray(new Object[]{0, 5}, 0));
    }
    
    public void testList(){
        {//numeric and string
            OlEval of = new OlEval();
            of.addExpr(0, new OlBExprEquals(1));
            of.addExpr(1, new OlBExprEquals(2));
            of.addExpr(2, new OlBExprEquals("abc"));
            short s = 2;
            assertTrue(of.evalList( Arrays.asList(new Object[]{new Long(1), new Short(s), "abc"}), 0));
            assertFalse(of.evalList(Arrays.asList(new Object[]{new Long(1), new Short(s), "abc1"}), 0));
            assertTrue(of.evalList(Arrays.asList(new Object[]{"dummy", new Long(1), new Short(s), "abc"}), 1));
        }        
    }
    
    public void testINExpr(){
        {
            OlEval of = new OlEval();
            of.addExpr(0, new OlBExprIN(new Object[]{6, 9, 12}));
            assertTrue(of.evalArray(new Object[]{6}, 0));
            assertTrue(of.evalArray(new Object[]{9}, 0));
            assertTrue(of.evalArray(new Object[]{12}, 0));
            assertFalse(of.evalArray(new Object[]{13}, 0));
        }
        //null
        {
            OlEval of = new OlEval();
            of.addExpr(0, new OlBExprIN(new Object[]{6, null, 12, new OlExprColumn(1)}));
            assertTrue(of.evalArray(new Object[]{6}, 0));
            assertFalse(of.evalArray(new Object[]{9, 10}, 0));
            assertTrue(of.evalArray(new Object[]{12}, 0));
            assertFalse(of.evalArray(new Object[]{13, 10}, 0));
            assertTrue(of.evalArray(new Object[]{null}, 0));
            assertTrue(of.evalArray(new Object[]{1, 1}, 0));
            assertFalse(of.evalArray(new Object[]{1, 2}, 0));
        }
    }
    
    public void testBETWEENExpr(){
        OlEval of = new OlEval();
        of.addExpr(0, new OlBExprBetween(5,8));
        assertTrue(of.evalArray(new Object[]{5}, 0));
        assertTrue(of.evalArray(new Object[]{6}, 0));
        assertTrue(of.evalArray(new Object[]{7}, 0));
        assertTrue(of.evalArray(new Object[]{8}, 0));
        assertFalse(of.evalArray(new Object[]{4}, 0));
        assertFalse(of.evalArray(new Object[]{9}, 0));
        assertFalse(of.evalArray(new Object[]{null}, 0));
    }
    
    public void testNotNullxpr(){
        OlEval of = new OlEval();
        of.addExpr(0, new OlBExprNotNull());
        assertFalse(of.evalArray(new Object[]{null}, 0));
        assertTrue(of.evalArray(new Object[]{1}, 0));
        assertTrue(of.evalArray(new Object[]{"aa"}, 0));
    }
    
    public void testGExpr(){
        {
            OlEval of = new OlEval();
            of.addExpr(0, new OlBExprGE(1));
            assertTrue(of.evalArray(new Object[]{2}, 0));
            assertTrue(of.evalArray(new Object[]{1}, 0));
            assertFalse(of.evalArray(new Object[]{0}, 0));
        }
        //test column
        {
            OlEval of = new OlEval();
            of.addExpr(0, new OlBExprGE( new OlExprColumn(1)));
            assertTrue(of.evalArray(new Object[]{2, 1}, 0));
            assertTrue(of.evalArray(new Object[]{1, 1}, 0));
            assertFalse(of.evalArray(new Object[]{0, 1}, 0));
        }
        //test null
        {
            OlEval of = new OlEval();
            of.addExpr(0, new OlBExprGE(new OlExprColumn(1)));
            assertFalse(of.evalArray(new Object[]{2, null}, 0));
            assertFalse(of.evalArray(new Object[]{null, null}, 0));
            assertFalse(of.evalArray(new Object[]{null, 2}, 0));
            assertTrue(of.evalArray(new Object[]{2, 1}, 0));
        }

    }
    public void testLExpr(){
        {
            OlEval of = new OlEval();
            of.addExpr(0, new OlBExprLE(1));
            assertFalse(of.evalArray(new Object[]{2}, 0));
            assertTrue(of.evalArray(new Object[]{1}, 0));
            assertTrue(of.evalArray(new Object[]{0}, 0));
        }
        //test column
        {
            OlEval of = new OlEval();
            of.addExpr(0, new OlBExprLE( new OlExprColumn(1)));
            assertFalse(of.evalArray(new Object[]{2, 1}, 0));
            assertTrue(of.evalArray(new Object[]{1, 1}, 0));
            assertTrue(of.evalArray(new Object[]{0, 1}, 0));
        }
    }    
    
    /**
     * comparison with Integer, Long
     */
    public void testValuedExpr(){
        {
            OlEval of = new OlEval();
            of.addExpr(0, new OlBExprEquals(null));
            assertTrue(of.evalArray(new Object[]{null}, 0));
            assertFalse(of.evalArray(new Object[]{1}, 0));
        }
        {
            OlEval of = new OlEval();
            of.addExpr(0, new OlBExprEquals(1));
            assertFalse(of.evalArray(new Object[]{null}, 0));
        }        
        {//two values in a row
            OlEval of = new OlEval();
            of.addExpr(0, new OlBExprEquals(null));
            of.addExpr(1, new OlBExprEquals(null));
            assertTrue(of.evalArray(new Object[]{null, null}, 0));
            assertFalse(of.evalArray(new Object[]{null, 1}, 0));
            assertFalse(of.evalArray(new Object[]{1, null}, 0));
        }
        {//numeric value
            OlEval of = new OlEval();
            of.addExpr(0, new OlBExprEquals(1));
            of.addExpr(1, new OlBExprEquals(2));
            assertTrue(of.evalArray(new Object[]{new Integer(1), new Long(2)}, 0));
            assertFalse(of.evalArray(new Object[]{new Integer(1), new Long(3)}, 0));
            assertFalse(of.evalArray(new Object[]{new Integer(2), new Long(2)}, 0));
            assertFalse(of.evalArray(new Object[]{new Long(1), new Long(2)}, 0));
            assertTrue(of.evalArray(new Object[]{new Integer(1), new Long(2)}, 0));
        }
        {//numeric and string
            OlEval of = new OlEval();
            of.addExpr(0, new OlBExprEquals(1));
            of.addExpr(1, new OlBExprEquals(2));
            of.addExpr(2, new OlBExprEquals("abc"));
            short s = 2;
            assertTrue(of.evalArray(new Object[]{new Long(1), new Short(s), "abc"}, 0));
            assertFalse(of.evalArray(new Object[]{new Long(1), new Short(s), "abc1"}, 0));
            assertTrue(of.evalArray(new Object[]{"dummy", new Long(1), new Short(s), "abc"}, 1));
        }
        
    }

}
