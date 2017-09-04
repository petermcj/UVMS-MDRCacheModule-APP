/*
Developed by the European Commission - Directorate General for Maritime Affairs and Fisheries @ European Union, 2015-2016.

This file is part of the Integrated Fisheries Data Management (IFDM) Suite. The IFDM Suite is free software: you can redistribute it
and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of
the License, or any later version. The IFDM Suite is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
details. You should have received a copy of the GNU General Public License along with the IFDM Suite. If not, see <http://www.gnu.org/licenses/>.
*/
package eu.europa.ec.fisheries.mdr.service.bean;

import eu.europa.ec.fisheries.mdr.entities.MdrConfiguration;
import eu.europa.ec.fisheries.mdr.repository.MdrRepository;
import eu.europa.ec.fisheries.mdr.service.MdrSchedulerService;
import eu.europa.ec.fisheries.mdr.service.MdrSynchronizationService;
import eu.europa.ec.fisheries.uvms.exception.ServiceException;
import java.util.Collection;
import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.ScheduleExpression;
import javax.ejb.Stateless;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;
import javax.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

/**
 * @author kovian
 *         <p>
 *         EJB that provides the MDR Synchronization Functionality.
 *         Methods for handeling the automatic job scheduler configuration.
 */
@Slf4j
@Stateless
@Transactional
public class MdrSchedulerServiceBean implements MdrSchedulerService {

    public static final String MDR_SYNCHRONIZATION_TIMER = "MDRSynchronizationTimer";
    private static final TimerConfig TIMER_CONFIG = new TimerConfig(MDR_SYNCHRONIZATION_TIMER, false);

    @EJB
    private MdrRepository mdrRepository;

    @EJB
    private MdrSynchronizationService synchBean;

    @Resource
    private TimerService timerServ;


    /**
     * Method that will be called when a timer has been set for this EJB.
     * When time runs up this method will trigger n requests (twoards FLUX TL, n being the nr. of available codeLists)
     * for the whole MDR registry update.
     */
    @Timeout
    public void timeOut() {
        log.info("\n\t---> STARTING SCHEDULED SYNCHRONIZATION OF MDR ENTITIES! \n");
        //synchBean.extractAcronymsAndUpdateMdr();
    }


    /**
     * Gets the actual MDR Synchronization Configuration;
     *
     * @return mdrSynch;
     */
    @Override
    public String getActualSchedulerConfiguration() {
        MdrConfiguration mdrSynch = mdrRepository.getMdrSchedulerConfiguration();
        return mdrSynch.getConfigValue();
    }

    /**
     * Reconfigures the scheduler and saves the new configuration to MDR Config Table.
     *
     * @param schedulerExpressionStr
     */
    @Override
    @Transactional(Transactional.TxType.REQUIRED)
    public void reconfigureScheduler(String schedulerExpressionStr) throws IllegalArgumentException {
        log.info("[START] Re-configure MDR scheduler with expression: {}", schedulerExpressionStr);
        String schedulerExpressionStrClean = null;
        if (StringUtils.isNotBlank(schedulerExpressionStr)) {
            // Sometimes unneeded double quotes are present in the posted json string
            schedulerExpressionStrClean = schedulerExpressionStr.replace("\"", "");
            try {
                // Set up the new timer for this EJB;
                setUpScheduler(schedulerExpressionStrClean);
                // Persist the new config into DB;
                mdrRepository.changeMdrSchedulerConfiguration(schedulerExpressionStrClean);
                log.info("New MDR scheduler timer created - [{}] - and stored.", TIMER_CONFIG.getInfo());
            } catch (ServiceException e) {
                log.error("Error while trying to save the new configuration", e);
            } catch (IllegalArgumentException ex) {
                throw ex;
            }
        } else {
            log.info("[FAILED] Re-configure MDR scheduler with expression: {}. The Scheduler expression is blank.", schedulerExpressionStrClean);
        }
    }

    /**
     * Given the schedulerExpressionStr creates a new timer for this bean.
     *
     * @param schedulerExpressionStr
     */
    @Override
    public void setUpScheduler(String schedulerExpressionStr) throws IllegalArgumentException {
        try {
            // Parse the Cron-Job expression;
            ScheduleExpression expression = parseExpression(schedulerExpressionStr);
            // Firstly, we need to cancel the current timer, if already exists one;
            cancelPreviousTimer();
            // Set up the new timer for this EJB;
            timerServ.createCalendarTimer(expression, TIMER_CONFIG);
            ;
        } catch (IllegalArgumentException ex) {
            log.warn("Error creating new scheduled synchronization timer!", ex);
            throw ex;
        }
        log.info("New timer scheduler created successfully : ", schedulerExpressionStr);
    }

    /**
     * Cancels the previous set up of the timer for this bean.
     */
    private void cancelPreviousTimer() {
        Collection<Timer> allTimers = timerServ.getTimers();
        for (Timer currentTimer : allTimers) {
            if (TIMER_CONFIG.getInfo().equals(currentTimer.getInfo())) {
                currentTimer.cancel();
                log.info("Current MDR scheduler timer cancelled.");
                break;
            }
        }
    }

    /**
     * Creates a ScheduleExpression object with the given schedulerExpressionStr String expression.
     *
     * @param schedulerExpressionStr
     * @return
     */
    private ScheduleExpression parseExpression(String schedulerExpressionStr) {
        ScheduleExpression expression = new ScheduleExpression();
        String[] args = schedulerExpressionStr.split("\\s");
        if (args.length != 5) {
            throw new IllegalArgumentException("Invalid scheduler expression: " + schedulerExpressionStr);
        }
        return expression.minute(args[0]).hour(args[1]).dayOfMonth(args[2]).month(args[3]).year(args[4]);
    }
}
