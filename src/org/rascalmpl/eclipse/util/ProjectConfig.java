package org.rascalmpl.eclipse.util;


import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.rascalmpl.eclipse.Activator;
import org.rascalmpl.library.util.PathConfig;
import org.rascalmpl.uri.ProjectURIResolver;
import org.rascalmpl.uri.URIUtil;
import org.rascalmpl.value.IList;
import org.rascalmpl.value.IListWriter;
import org.rascalmpl.value.ISourceLocation;
import org.rascalmpl.value.IValueFactory;

public class ProjectConfig {
    public static final String BIN_FOLDER = "bin";
    private final IValueFactory vf;
    
    public ProjectConfig(IValueFactory vf) {
        this.vf = vf;
    }
    
    public PathConfig getPathConfig(IProject project) {
        ISourceLocation projectLoc = ProjectURIResolver.constructProjectURI(project.getFullPath());
        
        RascalEclipseManifest manifest = new RascalEclipseManifest();
        
        IListWriter libPathWriter = vf.listWriter();
        
        // TODO: this needs to be configured elsewhere
        libPathWriter.append(URIUtil.correctLocation("std", "", ""));
        libPathWriter.append(URIUtil.correctLocation("plugin", "rascal_eclipse", "/src/org/rascalmpl/eclipse/library"));
        
        // These are jar files which make contain compiled Rascal code to link to:
        for (String lib : manifest.getRequiredLibraries(project)) {
            libPathWriter.append(URIUtil.getChildLocation(projectLoc, lib));
        }
        
        // These are other projects referenced by the current project for which we add
        // the bin folder to the lib path
        try {
            for (IProject ref : project.getReferencedProjects()) {
                libPathWriter.append(URIUtil.getChildLocation(ProjectURIResolver.constructProjectURI(ref.getFullPath()), BIN_FOLDER));
            }
        }
        catch (CoreException e) {
            Activator.log(e.getMessage(), e);
        }
        
        IList libPath = libPathWriter.done();
        
        IListWriter srcPathWriter = vf.listWriter();
        
        for (String src : manifest.getSourceRoots(project)) {
            ISourceLocation srcLoc = URIUtil.getChildLocation(projectLoc, src);
            srcPathWriter.append(srcLoc);
        }
        
        // TODO this is necessary while the kernel does not hold a compiled standard library, so remove later:
        srcPathWriter.append(URIUtil.correctLocation("std", "", ""));
        srcPathWriter.append(URIUtil.correctLocation("plugin", "rascal_eclipse", "/src/org/rascalmpl/eclipse/library"));
        
        IList srcPath = srcPathWriter.done();
        
        ISourceLocation binDir = URIUtil.getChildLocation(projectLoc, BIN_FOLDER);
        ISourceLocation bootDir = URIUtil.correctLocation("boot", "", "");
        
        return new PathConfig(srcPath, libPath, binDir, bootDir);
    }
}