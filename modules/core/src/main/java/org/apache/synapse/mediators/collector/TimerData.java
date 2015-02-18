package org.apache.synapse.mediators.collector;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.Timer;

public class TimerData {
	
	
	public static void addData(){

	Timer timer = new Timer(2000, new MyTimerActionListener());
		
	
	
	timer.start();
    try {
      Thread.sleep(10000);
    } catch (InterruptedException e) {
    }
    timer.stop();
	
	}
}
