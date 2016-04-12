package org.protege.editor.owl.ui.preferences;

import org.protege.editor.core.prefs.Preferences;
import org.protege.editor.core.prefs.PreferencesManager;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
/*
 * Copyright (C) 2007, University of Manchester
 *
 * Modifications to the initial code base are copyright of their
 * respective authors, or their employers as appropriate.  Authorship
 * of the modifications may be determined from the ChangeLog placed at
 * the end of this file.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.

 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.

 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */


/**
 * Author: Matthew Horridge<br>
 * The University Of Manchester<br>
 * Bio-Health Informatics Group<br>
 * Date: 14-Aug-2007<br><br>
 */
public class AnnotationPreferences {

    public static final String PREFERENCES_SET_KEY = "ANNOTATION_PREFS_SET";

    public static final String HIDDEN_URIS_KEY = "HIDDEN_ANNOATIONS_URIS";


    public static Set<URI> getHiddenAnnotationURIs() {
        PreferencesManager prefMan = PreferencesManager.getInstance();
        Preferences prefs = prefMan.getPreferencesForSet(PREFERENCES_SET_KEY, HIDDEN_URIS_KEY);
        Set<URI> uris = new HashSet<URI>();
        for (String s : prefs.getStringList(HIDDEN_URIS_KEY, new ArrayList<String>())) {
            uris.add(URI.create(s));
        }
        return uris;
    }


    public static void setHiddenAnnotationURIs(Set<URI> uris) {
        PreferencesManager prefMan = PreferencesManager.getInstance();
        Preferences prefs = prefMan.getPreferencesForSet(PREFERENCES_SET_KEY, HIDDEN_URIS_KEY);
        List<String> list = new ArrayList<String>();
        for (URI uri : uris) {
            list.add(uri.toString());
        }
        prefs.putStringList(HIDDEN_URIS_KEY, list);
    }
}
