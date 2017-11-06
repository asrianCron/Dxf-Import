// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.dxfimport;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryIteratorException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.testutils.JOSMTestRules;
import org.openstreetmap.josm.tools.Logging;

/**
 * Unit tests of {@link DxfImportTask} class.
 */
public class DxfImportTaskTest {

    /**
     * Setup test.
     */
    @Rule
    public JOSMTestRules rules = new JOSMTestRules().main().preferences().projection().timeout(120000);

    /**
     * Lists all datasets files matching given extension.
     * @param ext file extension to search for
     * @returns List of all datasets files matching given extension
     * @throws IOException in case of I/O error
     */
    public static Collection<Path> listDataFiles(String ext) throws IOException {
        return addTree(Paths.get(TestUtils.getTestDataRoot()), new ArrayList<>(), ext.toLowerCase(Locale.ENGLISH));
    }

    static Collection<Path> addTree(Path directory, Collection<Path> all, String ext) throws IOException {
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(directory)) {
            for (Path child : ds) {
                if (Files.isDirectory(child)) {
                    addTree(child, all, ext);
                } else if (child.toString().toLowerCase().endsWith(ext)) {
                    all.add(child);
                }
            }
        } catch (DirectoryIteratorException ex) {
            // I/O error encounted during the iteration, the cause is an IOException
            throw ex.getCause();
        }
        return all;
    }

    @Test
    public void testDxfImportTask() throws Exception {
        OsmDataLayer layer = new OsmDataLayer(new DataSet(), "", null);
        MainApplication.getLayerManager().addLayer(layer);
        try {
            for (Path p : listDataFiles("dxf")) {
                doTestDxfFile(p.toFile());
            }
        } finally {
            MainApplication.getLayerManager().removeLayer(layer);
        }
    }

    private static void doTestDxfFile(File file) throws InterruptedException, ExecutionException, TimeoutException {
        Logging.info("Importing file "+file.getPath()+"...");
        DxfImportTask task = new DxfImportTask(file);
        Future<?> future = MainApplication.worker.submit(task);
        assertNull(future.get(15, TimeUnit.SECONDS));
        assertNotNull(task.resultCommand);
        Logging.info("OK");
    }
}
