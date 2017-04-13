/*
Developed by the European Commission - Directorate General for Maritime Affairs and Fisheries @ European Union, 2015-2016.

This file is part of the Integrated Fisheries Data Management (IFDM) Suite. The IFDM Suite is free software: you can redistribute it 
and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of 
the License, or any later version. The IFDM Suite is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; 
without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more 
details. You should have received a copy of the GNU General Public License along with the IFDM Suite. If not, see <http://www.gnu.org/licenses/>.

 */
package eu.europa.ec.fisheries.mdr.mapper;

import eu.europa.ec.fisheries.mdr.entities.codelists.baseentities.MasterDataRegistry;
import eu.europa.ec.fisheries.mdr.exception.MdrCacheInitException;
import eu.europa.ec.fisheries.mdr.util.ClassFinder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;

import javax.annotation.PostConstruct;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/***
 * This class is designed to perform a scanning of the MDR Entities Package and build a cache to furtherly 
 * be used to construct request objects to send request to FLUX.
 *
 */
@Slf4j
public class MasterDataRegistryEntityCacheFactory {

	public static final String FAILED_TO_INITIATE_CACHE_FACTORY = "Failed to initiate MasterDataRegistryEntityCacheFactory class.";
	private static Map<String, MasterDataRegistry> acronymsCache;
	private static List<String> acronymsList;
	
	private static MasterDataRegistryEntityCacheFactory instance;
	private static final String METHOD_ACRONYM   = "getAcronym";
	private static final String ENTITIES_PACKAGE = "eu.europa.ec.fisheries.mdr.entities";

	static {
		try{
			initializeCache();
		} catch (Exception ex){
			log.error("Exception thrown while trying to initialize MasterDataRegistryEntityCacheFactory Entity Cache!", ex);
		}
	}

	@PostConstruct
	private static void initializeClass(){
		instance = new MasterDataRegistryEntityCacheFactory();
		try {
			log.info("Initializing MasterDataRegistryEntityCacheFactory class.");
			initializeCache();
		} catch (MdrCacheInitException e) {
			log.error(FAILED_TO_INITIATE_CACHE_FACTORY, e);
		}

	}

	public static MasterDataRegistryEntityCacheFactory getInstance(){
		if(instance == null){
			instance = new MasterDataRegistryEntityCacheFactory();
			try {
				initializeCache();
			} catch (MdrCacheInitException e) {
				log.error(FAILED_TO_INITIATE_CACHE_FACTORY, e);
			}
		}
		return instance;
	}

	/**
	 *
	 * @param entityAcronym
	 * @return always an Entity instance, no null values will be returned.
	 * @throws NullPointerException in case no valid entityAcronym is provided.
	 * @throws InvocationTargetException
	 * @throws NoSuchMethodException
	 * @throws MdrCacheInitException
	 * @throws IllegalArgumentException when the specified acronym cannot be found in the initialized entities cache
     */
	public MasterDataRegistry getNewInstanceForEntity(String entityAcronym) throws MdrCacheInitException {
		try {
			if(MapUtils.isEmpty(acronymsCache)){
				initializeCache();
			}

			if (acronymsCache.get(entityAcronym) == null) {
				throw new IllegalArgumentException("Code list '" + entityAcronym + "' is not recognized.");
			}

			return acronymsCache.get(entityAcronym).getClass().newInstance();
		} catch (InstantiationException | IllegalAccessException e) {
			throw new MdrCacheInitException(e);
		}
	}

	public boolean existsAcronym(String acronym){
		try {
			getNewInstanceForEntity(acronym);
		} catch (MdrCacheInitException | IllegalArgumentException e) {
			return false;
		}
		return true;
	}
	
	private static void initializeCache() throws MdrCacheInitException {
		if(MapUtils.isEmpty(acronymsCache)){
			List<Class<? extends MasterDataRegistry>> entitiesList = null;
			try {
				entitiesList = ClassFinder.extractEntityInstancesFromPackage();
			} catch(Exception ex){
				log.error("Couldn't extract entities from package "+ENTITIES_PACKAGE+" \n The following exception was thrown : \n", ex);
				throw new MdrCacheInitException(ex);
			}
			acronymsCache = new HashMap<>();
			acronymsList  = new ArrayList<>();
			for (Class<? extends  MasterDataRegistry> aClass : entitiesList) {
				if(!Modifier.isAbstract(aClass.getModifiers())){
					addAcronymToCache(aClass);
				}
			}
		}
	}

	/**
	 * Adds the given Entity (aka CodeList) to the cache.
	 *
	 * @param aClass
	 * @throws MdrCacheInitException
     */
	private static void addAcronymToCache(Class<? extends MasterDataRegistry> aClass) throws MdrCacheInitException {
		String classAcronym   			  = null;
		MasterDataRegistry classReference = null;
		try {
            classAcronym = (String) aClass.getMethod(METHOD_ACRONYM).invoke(aClass.newInstance());
            classReference =  aClass.newInstance();
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException | InstantiationException e) {
            throw new MdrCacheInitException("Exeption thrown while trying to initialize acronymsCache.", e);
        }
		acronymsCache.put(classAcronym, classReference);
		acronymsList.add(classAcronym);
		log.info("Creating cache instance for : " + aClass.getCanonicalName());
	}

	/**
	 * Returns the List of all available Acronyms fro MDR.
	 * 
	 * @return acronymsList
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws IllegalArgumentException
	 * @throws InvocationTargetException
	 * @throws NoSuchMethodException
	 * @throws SecurityException
	 */
	public static List<String> getAcronymsList() throws MdrCacheInitException {
		if(CollectionUtils.isEmpty(acronymsList)){
			initializeCache();
		} else {
			log.info("Getting acronyms from cached instance!");
		}
		return acronymsList;
	}

	/**
	 * Initializes the cache.
	 *
	 */
	public static void initialize() {
		try {
			initializeCache();
		} catch (MdrCacheInitException ex) {
			log.error("Error while trying to initialize MasterDataRegistryEntityCacheFactory.",ex);
		}
	}
}