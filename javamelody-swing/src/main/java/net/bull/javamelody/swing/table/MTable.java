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
package net.bull.javamelody.swing.table;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.SwingUtilities;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;

import net.bull.javamelody.swing.print.MClipboardPrinter;

/**
 * Composant Table, qui utilise une API typée pour la liste des valeurs et qui utilise les String pour définir les valeurs dans les colonnes.
 *
 * @param <T>
 *           Type des valeurs de la liste
 * @author Emeric Vernat
 */
public class MTable<T> extends MListTable<T> {
	static final Color BICOLOR_LINE = Color.decode("#E7E7E7");

	private static final long serialVersionUID = 1L;

	@SuppressWarnings("all")
	private static final KeyAdapter CLIPBOARD_KEY_LISTENER = new KeyAdapter() {
		@Override
		public void keyPressed(final KeyEvent event) {
			if (event.getSource() instanceof MTable) {
				final MTable<?> table = (MTable<?>) event.getSource();
				final int keyCode = event.getKeyCode();
				final int modifiers = event.getModifiers();
				if ((modifiers & java.awt.Event.CTRL_MASK) != 0 && keyCode == KeyEvent.VK_C) {
					event.consume();
					new MClipboardPrinter().print(table, null);
				}
			}
		}
	};

	@SuppressWarnings("all")
	private static final MouseAdapter POPUP_MENU_MOUSE_LISTENER = new MouseAdapter() {
		@Override
		public void mouseReleased(final MouseEvent event) {
			if (event.isPopupTrigger() || SwingUtilities.isRightMouseButton(event)
					&& event.getComponent() instanceof MTable) {
				final MBasicTable table = (MBasicTable) event.getComponent();
				final TablePopupMenu tablePopupMenu = new TablePopupMenu(table);
				tablePopupMenu.show(table, event.getX(), event.getY());
			}
		}
	};

	/**
	 * Constructeur.
	 */
	public MTable() {
		// on utilise le modèle par défaut créé par la méthode createDefaultDataModel() ci-dessus
		this(null);
	}

	/**
	 * Constructeur.
	 *
	 * @param dataModel
	 *           Modèle pour les données
	 */
	protected MTable(final MListTableModel<T> dataModel) {
		super(dataModel);
		// la table ne crée pas automatiquement les colonnes à partir du dataModel,
		// et les colonnes seront définies en utilisant la méthode addColumn ci-dessous
		setAutoCreateColumnsFromModel(false);
		// le rowSorter sera automatiquement ajouté lors des appels à setModel (la classe MListTableModel définit getColumnClass à partir des données)
		setAutoCreateRowSorter(true);
		// fond de couleur blanc (plutôt que gris en look and feel Nimbus ; utile pour le rendu des cases à cocher dans le tableau)
		setBackground(Color.WHITE);
		// listener pour surcharger la copie dans presse-papier (format pour Excel et non format par défaut de Swing)
		addKeyListener(CLIPBOARD_KEY_LISTENER);
		// listener pour afficher le popup menu
		addMouseListener(POPUP_MENU_MOUSE_LISTENER);
	}

	@Override
	protected TableModel createDefaultDataModel() {
		return new MTableModel<T>(this);
	}

	/**
	 * Ajoute une colonne dans la table.
	 *
	 * @param attribute
	 *           Nom de l'attribut des objets à afficher dans la colonne<br/>
	 * @param libelle
	 *           Libellé à afficher en entête de la colonne
	 * @return this (fluent)
	 */
	public MTable<T> addColumn(final String attribute, final String libelle) {
		final int modelIndex = getColumnCount();
		final TableColumn tableColumn = new TableColumn(modelIndex);
		// on met l'énumération de l'attribut comme identifier dans le TableColumn pour s'en servir dans MTableModel
		tableColumn.setIdentifier(attribute);
		if (libelle == null) {
			// on prend par défaut l'attribut si le libellé n'est pas précisé,
			tableColumn.setHeaderValue(attribute);
		} else {
			// le libellé a été précisé pour l'entête de cette colonne
			tableColumn.setHeaderValue(libelle);
		}
		// ajoute la colonne dans la table
		super.addColumn(tableColumn);
		return this;
	}

	/**
	 * Méthode typée pour définir un renderer spécifique pour les cellules d'une colonne dans la table.
	 *
	 * @param attribute
	 *           Nom de l'attribut des objets pour une colonne existante
	 * @param cellRenderer
	 *           Renderer des cellules dans cette colonne
	 */
	public void setColumnCellRenderer(final String attribute,
			final TableCellRenderer cellRenderer) {
		getColumn(attribute).setCellRenderer(cellRenderer);
	}

	/** {@inheritDoc} */
	@Override
	public Component prepareRenderer(final TableCellRenderer renderer, final int rowIndex,
			final int vColIndex) {
		// Surcharge pour la gestion des lignes de couleurs alternées.
		final Component component = super.prepareRenderer(renderer, rowIndex, vColIndex);
		if (BICOLOR_LINE != null && !isRowSelected(rowIndex)) {
			if (rowIndex % 2 == 0) {
				component.setBackground(BICOLOR_LINE);
			} else {
				component.setBackground(getBackground());
			}
		}

		return component;
	}
}
