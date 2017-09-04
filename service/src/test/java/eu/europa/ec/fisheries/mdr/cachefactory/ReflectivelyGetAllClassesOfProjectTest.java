/*
Developed by the European Commission - Directorate General for Maritime Affairs and Fisheries @ European Union, 2015-2016.

This file is part of the Integrated Fisheries Data Management (IFDM) Suite. The IFDM Suite is free software: you can redistribute it
and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of
the License, or any later version. The IFDM Suite is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
details. You should have received a copy of the GNU General Public License along with the IFDM Suite. If not, see <http://www.gnu.org/licenses/>.

*/
package eu.europa.ec.fisheries.mdr.cachefactory;

import eu.europa.ec.fisheries.mdr.util.ClassFinder;
import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import lombok.SneakyThrows;
import org.junit.Ignore;
import org.junit.Test;
import org.reflections.Reflections;
import org.reflections.scanners.ResourcesScanner;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.FilterBuilder;

/**
 * Created by kovian on 28/11/2016.
 */
public class ReflectivelyGetAllClassesOfProjectTest {

    private final static String ERS_BASE_PACKAGE  = "eu.europa.ec.fisheries.ers";
    private final static String MDR_BASE_PACKAGE  = "eu.europa.ec.fisheries.mdr";

    @Test
    @SneakyThrows
    public void testGetAllPackages(){
        final Set<String> packageNamesForErs = findAllForPackages(ERS_BASE_PACKAGE, MDR_BASE_PACKAGE);
        System.out.println("I found : " + packageNamesForErs.size() + " Mdr related packages in this project!");
        logPackages(packageNamesForErs);
        System.out.println("\n Getting class refferences for each package! This may take a while..\n");
        final List<Class<?>> classes = new ArrayList<>();
        for(String actPackage : packageNamesForErs){
            classes.addAll(ClassFinder.extractClassesForPackageWithScanner(actPackage));
        }
        logClasses(classes);
        System.out.println("In the end i found : " + classes.size() + " classes in the whole project!");
    }

    private void logClasses(List<Class<?>> classes) {
        for(Class<?> actclass : classes){
         System.out.println(" Class : " + actclass.getCanonicalName());
        }

    }

    public Set<String> findAllForPackages(String... packagesLike){
        Set<String> packageNames = new HashSet<>();
        for(String packageLike : packagesLike){
            packageNames.addAll(findAllPackagesStartingWith(packageLike));
        }
        return packageNames;
    }

    private void logPackages(Set<String> packageNamesForErs) {
        System.out.println("\n-------------------------------------------------------------------\n");
        int i = 0;
        for(String actPackage : packageNamesForErs){
            System.out.println("    " + i++ + ". Package : " + actPackage);
        }
        System.out.println("\n-------------------------------------------------------------------\n");
    }

    public Set<String> findAllPackagesStartingWith(String prefix) {
        List<ClassLoader> classLoadersList = new LinkedList<>();
        classLoadersList.add(ClasspathHelper.contextClassLoader());
        classLoadersList.add(ClasspathHelper.staticClassLoader());
        Reflections reflections = new Reflections(new ConfigurationBuilder()
                .setScanners(new SubTypesScanner(false), new ResourcesScanner())
                .setUrls(ClasspathHelper.forClassLoader(classLoadersList.toArray(new ClassLoader[0])))
                .filterInputsBy(new FilterBuilder().include(FilterBuilder.prefix("eu.europa.ec"))));
        Set<Class<? extends Object>> classes = reflections.getSubTypesOf(Object.class);

        Set<String> packageNameSet = new TreeSet<>();
        for (Class classInstance : classes) {
            String packageName = classInstance.getPackage().getName();
            if (packageName.startsWith(prefix)) {
                packageNameSet.add(packageName);
            }
        }
        return packageNameSet;
    }

    @SneakyThrows
    @Test
    @Ignore
    public void testIfItWorks(){
        final List<String> strings = extractAllFilesPaths("C:\\GIT Repository\\activity-trunk\\uvms-activity-app", new ArrayList<String>());
        System.out.print("OK");
    }

    private List<String> extractAllFilesPaths(String baseDir, List<String> filePathsList) {
        File pathAsFile = new File(baseDir);
        File[] filesList = pathAsFile.listFiles();
        for(File actFile : filesList){
            if(actFile.isDirectory()){
                extractAllFilesPaths(actFile.getAbsolutePath(), filePathsList);
            }
            filePathsList.add(actFile.getAbsolutePath());
        }
        return filePathsList;
    }
}
