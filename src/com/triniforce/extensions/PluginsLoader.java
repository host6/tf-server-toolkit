/*
 * Copyright(C) Triniforce
 * All Rights Reserved.
 *
 */ 
package com.triniforce.extensions;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.triniforce.server.srvapi.IPlugin;
import com.triniforce.utils.ApiAlgs;
import com.triniforce.utils.TFUtils;

public class PluginsLoader {
    
    public static String trimVersion(String name){
        Pattern pattern = Pattern.compile("-\\d.*");
        Matcher matcher = pattern.matcher(name);
        if(!matcher.find()){
            return name;
        }
        return name.substring(0, matcher.start());
        
    }
    
    private final File m_folder;

    public PluginsLoader(File folder) {
        m_folder = folder;
    }
    
    public List<File> getJarFiles(){
        List<File> res = new ArrayList<File>();
        
        if(! m_folder.exists() || !m_folder.isDirectory()){
            return res;
        }
        
        for(String fileName: m_folder.list()){
            File jarFile = new File(m_folder, fileName);
            if(jarFile.isFile() && fileName.endsWith(".jar")){
                res.add(jarFile);
            }
        }
        return res;
    }
    
    public String readPluginClass(File jar){
        String res ="";
        try {
            JarFile jf = new JarFile(jar);
            try{
                res = jf.getManifest().getMainAttributes().getValue("Main-Class");
                if(null == res){
                    res = jf.getManifest().getMainAttributes().getValue("Pk-Plugin-Main-Class");
                }
            }finally{
                jf.close();
            }
        } catch (Exception e) {
            ApiAlgs.getLog(this).error("Error reading manifest" + jar,e);
            res ="";
        }        
        if(null == res){
            res ="";
        }
        return res;
    }
    
    
    public Collection<Class> loadClasses(){
        List<Class> res = new ArrayList<Class>();
        
        List<URL> urls = new ArrayList<URL>();
        List<String> mainClasses = new ArrayList<String>();
        
        for(File jar: getJarFiles()){
            String mainClass = readPluginClass(jar);
            if(!TFUtils.isEmptyString(mainClass)){
                mainClasses.add(mainClass);
            }
            try {
                urls.add(jar.toURI().toURL());
            } catch (Exception e){
                ApiAlgs.rethrowException(e);
            }            
        }
        
		@SuppressWarnings("resource")
		URLClassLoader ucl = new URLClassLoader(urls.toArray(new URL[urls.size()]), this.getClass().getClassLoader());
        for(String mainClass: mainClasses){
            try {
                Class cls = ucl.loadClass(mainClass);
                res.add(cls);
            } catch (Exception e) {
                ApiAlgs.rethrowException(e);
            }
        }
        return res;        
    }
    
    
    public List<IPlugin> loadPlugins(){
        List<IPlugin> res = new ArrayList<IPlugin>();

        for(Class cls: loadClasses()){
            try {
                if(IPlugin.class.isAssignableFrom(cls)){
                    IPlugin plugin = (IPlugin) cls.newInstance();
                    res.add(plugin);
                }
            } catch (Exception e) {
                ApiAlgs.rethrowException(e);
            }
        }
        return res;
    }

}
