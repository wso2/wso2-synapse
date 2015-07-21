package org.apache.synapse.aspects.newstatistics;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.TimerTask;

/**
 * StatisticStoreCleanerHandler execute at statistics clean repeatedly and clean statistics
 * store if statistics cleaning is enabled
 */
public class StatisticStoreCleanerHandler extends TimerTask {

	private static final Log log = LogFactory.getLog(StatisticStoreCleanerHandler.class);

	/**
	 * a lock to prevent concurrent execution
	 */
	private final Object lock = new Object();
	private boolean alreadyExecuting = false;

	StatisticsStoreCleaner statisticsStoreCleaner;

	/**
	 * Sets statisticsStoreCleaner to be executed at specified timeout
	 *
	 * @param statisticsStoreCleaner statistics cleaning object to process
	 */
	public StatisticStoreCleanerHandler(StatisticsStoreCleaner statisticsStoreCleaner) {
		this.statisticsStoreCleaner = statisticsStoreCleaner;
	}

	/**
	 * Checks if the timeout has expired for each cleaning Statistics in the Statistics store. If
	 * expired, removes existing Statistics.
	 */
	@Override public void run() {
		if (alreadyExecuting) {
			return;
		}
		synchronized (lock) {
			alreadyExecuting = true;
			try {
				cleanStatistics();
			} catch (Exception ex) {
				log.warn("Exception occurred while cleaning the statistics", ex);
			} catch (Error ex) {
				log.warn("Error occurred while cleaning the statistics", ex);
			} finally {
				alreadyExecuting = false;
			}
		}
	}

	/**
	 * Cleans Statistics in the statistic store
	 */
	private void cleanStatistics() {
		statisticsStoreCleaner.clean();
	}
}
