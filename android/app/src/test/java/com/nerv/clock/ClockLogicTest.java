package com.nerv.clock;

import org.junit.Test;
import org.junit.Before;
import org.junit.After;
import static org.junit.Assert.*;

public class ClockLogicTest {
    
    private ClockLogic clock;
    
    @Before
    public void setUp() {
        clock = new ClockLogic();
    }
    
    @After
    public void tearDown() {
        clock = null;
    }
    
    @Test
    public void testInitialModeIsNormal() {
        assertEquals(ClockLogic.Mode.NORMAL, clock.getCurrentMode());
    }
    
    @Test
    public void testInitialStateNotPaused() {
        assertFalse(clock.isPaused());
    }
    
    @Test
    public void testInitialStateNotDepleted() {
        assertFalse(clock.isDepleted());
    }
    
    @Test
    public void testInitialWarningStateIsNormal() {
        assertEquals(ClockLogic.WarningState.NORMAL, clock.getWarningState());
    }
    
    @Test
    public void testSetModeToRacing() {
        clock.setMode(ClockLogic.Mode.RACING);
        assertEquals(ClockLogic.Mode.RACING, clock.getCurrentMode());
    }
    
    @Test
    public void testSetModeToSlow() {
        clock.setMode(ClockLogic.Mode.SLOW);
        assertEquals(ClockLogic.Mode.SLOW, clock.getCurrentMode());
    }
    
    @Test
    public void testSetModeToNormal() {
        clock.setMode(ClockLogic.Mode.RACING);
        clock.setMode(ClockLogic.Mode.NORMAL);
        assertEquals(ClockLogic.Mode.NORMAL, clock.getCurrentMode());
    }
    
    @Test
    public void testTogglePauseInRacingMode() {
        clock.setMode(ClockLogic.Mode.RACING);
        assertFalse(clock.isPaused());
        
        clock.togglePause();
        assertTrue(clock.isPaused());
        
        clock.togglePause();
        assertFalse(clock.isPaused());
    }
    
    @Test
    public void testTogglePauseInSlowMode() {
        clock.setMode(ClockLogic.Mode.SLOW);
        assertFalse(clock.isPaused());
        
        clock.togglePause();
        assertTrue(clock.isPaused());
    }
    
    @Test
    public void testCannotPauseNormalMode() {
        clock.setMode(ClockLogic.Mode.NORMAL);
        clock.togglePause();
        assertFalse(clock.isPaused());
    }
    
    @Test
    public void testPomodoroDurationIs25MinInitially() {
        clock.setMode(ClockLogic.Mode.SLOW);
        assertEquals(25 * 60 * 1000, clock.getPomodoroDuration());
    }
    
    @Test
    public void testTogglePomodoroDuration() {
        clock.setMode(ClockLogic.Mode.SLOW);
        long firstDuration = clock.getPomodoroDuration();
        
        clock.setMode(ClockLogic.Mode.SLOW);
        long secondDuration = clock.getPomodoroDuration();
        
        assertNotEquals(firstDuration, secondDuration);
        assertEquals(15 * 60 * 1000, secondDuration);
    }
    
    @Test
    public void testGetHourString() {
        String hourStr = clock.getHourString();
        assertNotNull(hourStr);
        assertEquals(2, hourStr.length());
    }
    
    @Test
    public void testGetMinuteString() {
        String minStr = clock.getMinuteString();
        assertNotNull(minStr);
        assertEquals(2, minStr.length());
    }
    
    @Test
    public void testGetSecondString() {
        String secStr = clock.getSecondString();
        assertNotNull(secStr);
        assertEquals(2, secStr.length());
    }
    
    @Test
    public void testGetCentisecondString() {
        String csStr = clock.getCentisecondString();
        assertNotNull(csStr);
        assertEquals(2, csStr.length());
    }
    
    @Test
    public void testResetLastUpdateTime() {
        long before = clock.getPomodoroRemaining();
        clock.resetLastUpdateTime();
    }
}
