/*
 * Copyright(C) Triniforce
 * All Rights Reserved.
 *
 */ 
package com.triniforce.syncbus;

public interface ISubscriber{
    SubscrResponse handleEvent(Object addr, SubscrData data);  
}