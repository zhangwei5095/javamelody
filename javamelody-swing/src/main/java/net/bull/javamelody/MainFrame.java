/*
 * Copyright 2008-2016 by Emeric Vernat
 *
 *     This file is part of Java Melody.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.bull.javamelody;

import java.awt.AWTEvent;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.color.ColorSpace;
import java.awt.event.AWTEventListener;
import java.awt.image.BufferedImageOp;
import java.awt.image.ColorConvertOp;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import net.bull.javamelody.swing.util.FastBlurFilter;
import net.bull.javamelody.swing.util.JXLayer;
import net.bull.javamelody.swing.util.MSwingUtilities;

class MainFrame extends JFrame {
	/**
	 * Délai d'inactivité en minute avant masquage des données.
	 */
	private static final int INACTIVITY_DELAY = 30;
	private static final long serialVersionUID = 1L;
	long lastActivity;

	MainFrame() {
		super();
		setTitle("Java Melody");
		setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		setIconImage(ImageIconCache.getImageIcon("systemmonitor.png").getImage());
		// définit la taille
		//		pack();
		//		setLocationRelativeTo(null);
		setBounds(getDefaultBounds());
		setExtendedState(Frame.MAXIMIZED_BOTH);
		initActivityMonitor();
	}

	/**
	 * Retourne les bounds par défaut du desktop en fonction de la résolution de l'écran et du menu.
	 * @return Rectangle
	 */
	private Rectangle getDefaultBounds() {
		final Dimension size = getToolkit().getScreenSize();
		final Insets insets = getToolkit().getScreenInsets(getGraphicsConfiguration());
		return new Rectangle(insets.left, insets.top, size.width - insets.left - insets.right,
				size.height - insets.top - insets.bottom);
	}

	public static boolean showConfirmation(Component component, String message) {
		final Window window = SwingUtilities.getWindowAncestor(component);
		if (window instanceof MainFrame) {
			final MainFrame mainFrame = (MainFrame) window;
			try {
				mainFrame.setBlurContentPane(true);
				return MSwingUtilities.showConfirmation(component, message);
			} finally {
				mainFrame.setBlurContentPane(false);
			}
		}
		return MSwingUtilities.showConfirmation(component, message);
	}

	public static void showMessage(Component component, String message) {
		final Window window = SwingUtilities.getWindowAncestor(component);
		if (window instanceof MainFrame) {
			final MainFrame mainFrame = (MainFrame) window;
			try {
				mainFrame.setBlurContentPane(true);
				MSwingUtilities.showMessage(component, message);
			} finally {
				mainFrame.setBlurContentPane(false);
			}
		} else {
			MSwingUtilities.showMessage(component, message);
		}
	}

	public static void showException(Component component, Throwable throwable) {
		final Window window = SwingUtilities.getWindowAncestor(component);
		if (window instanceof MainFrame) {
			final MainFrame mainFrame = (MainFrame) window;
			try {
				mainFrame.setGrayContentPane(true);
				MSwingUtilities.showException(throwable);
			} finally {
				mainFrame.setGrayContentPane(false);
			}
		} else {
			MSwingUtilities.showException(throwable);
		}
	}

	@SuppressWarnings("all")
	final void exit(int exitCode) {
		System.exit(exitCode); // NOPMD
	}

	@Override
	public void setContentPane(Container contentPane) {
		if (contentPane instanceof JComponent) {
			super.setContentPane(new JXLayer((JComponent) contentPane));
		} else {
			super.setContentPane(contentPane);
		}
	}

	/**
	 * Grise ou dégrise le contentPane de la frame principale.
	 * @param gray boolean
	 */
	public void setGrayContentPane(boolean gray) {
		final BufferedImageOp bio = new ColorConvertOp(ColorSpace.getInstance(ColorSpace.CS_GRAY),
				null);
		((JXLayer) getContentPane()).setBufferedImageOp(gray ? bio : null);
	}

	/**
	 * Floute ou défloute le contentPane de la frame principale.
	 * @param blur boolean
	 */
	public void setBlurContentPane(boolean blur) {
		final JXLayer jxLayer = (JXLayer) getContentPane();
		if (blur && jxLayer.getBufferedImageOp() instanceof FastBlurFilter
				|| !blur && !(jxLayer.getBufferedImageOp() instanceof FastBlurFilter)) {
			return;
		}
		final BufferedImageOp bio = new FastBlurFilter(1);
		jxLayer.setBufferedImageOp(blur ? bio : null);
	}

	private void initActivityMonitor() {
		lastActivity = System.currentTimeMillis();
		final AWTEventListener awtEventListener = new AWTEventListener() {
			/** {@inheritDoc} */
			@Override
			public void eventDispatched(AWTEvent event) {
				lastActivity = System.currentTimeMillis();
			}
		};
		Toolkit.getDefaultToolkit().addAWTEventListener(awtEventListener,
				AWTEvent.KEY_EVENT_MASK | AWTEvent.MOUSE_EVENT_MASK
						| AWTEvent.MOUSE_MOTION_EVENT_MASK | AWTEvent.MOUSE_WHEEL_EVENT_MASK);
		final Timer activityMonitorTimer = new Timer("ActivityMonitorTimer", true);
		final TimerTask timerTask = new TimerTask() {
			/** {@inheritDoc} */
			@Override
			public void run() {
				if (System.currentTimeMillis() - lastActivity > INACTIVITY_DELAY * 60L * 1000) {
					exit(1);
				}
			}
		};
		// on vérifie l'activité utilisateur toutes les 60 secondes par rapport au délai maximum d'inactivité
		activityMonitorTimer.schedule(timerTask, 60L * 1000, 60L * 1000);
	}
}
