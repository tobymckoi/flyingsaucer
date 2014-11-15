/*
 * PanelManager.java
 * Copyright (c) 2005 Torbjoern Gannholm
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 2.1
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.	See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 *
 */
package org.xhtmlrenderer.demo.aboutbox;

import java.io.StringReader;
import java.net.URL;
import java.util.ArrayList;
import org.xhtmlrenderer.extend.AbstractUserAgent;

import org.xhtmlrenderer.resource.DocumentResource;
import org.xhtmlrenderer.resource.XMLDocumentResource;
import org.xhtmlrenderer.util.Uu;


/**
 * Created by IntelliJ IDEA.
 * User: tobe
 * Date: 2005-jun-15
 * Time: 07:38:59
 * To change this template use File | Settings | File Templates.
 */
public class DemoUserAgent extends AbstractUserAgent {

    private int index = -1;
    private ArrayList history = new ArrayList();



    @Override
    public DocumentResource getDocumentResource(String uri) {
        DocumentResource xr = super.getDocumentResource(uri);
        if (xr == null) {
            String notFound = "<h1>Document not found</h1>";
            xr = XMLDocumentResource.load(uri, new StringReader(notFound));
        }
        return xr;
    }

    @Override
    public boolean isVisited(String uri) {
        if (uri == null) return false;
        uri = resolveURI(uri);
        return history.contains(uri);
    }

    @Override
    public void setBaseURL(String url) {
        String resolvedUrl = resolveURI(url);
        if (resolvedUrl == null) resolvedUrl = "error:FileNotFound";
        super.setBaseURL(resolvedUrl);
        //setBaseURL is called by view when document is loaded
        if (index >= 0) {
            String historic = (String) history.get(index);
            if (historic.equals(resolvedUrl)) return;//moved in history
        }
        index++;
        for (int i = index; i < history.size(); history.remove(i)) ;
        history.add(index, resolvedUrl);
    }

    @Override
    public String resolveURI(String uri) {
        URL ref = null;
        if (uri == null) return getBaseURL();
        if (uri.trim().equals("")) return getBaseURL();//jar URLs don't resolve this right
        if (uri.startsWith("demo:")) {
            DemoMarker marker = new DemoMarker();
            String short_url = uri.substring(5);
            if (!short_url.startsWith("/")) {
                short_url = "/" + short_url;
            }
            ref = marker.getClass().getResource(short_url);
            Uu.p("ref = " + ref);

            if (ref == null)
                return null;
            else
                return ref.toExternalForm();

        } else {
            return super.resolveURI(uri);
        }

    }



    public String getForward() {
        index++;
        return (String) history.get(index);
    }

    public String getBack() {
        index--;
        return (String) history.get(index);
    }

    public boolean hasForward() {
        if (index + 1 < history.size() && index >= 0) {
            return true;
        } else {
            return false;
        }
    }

    public boolean hasBack() {
        if (index >= 0) {
            return true;
        } else {
            return false;
        }
    }
}
