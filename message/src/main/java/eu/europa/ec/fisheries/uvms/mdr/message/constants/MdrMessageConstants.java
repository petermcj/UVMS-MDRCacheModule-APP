/*
Developed by the European Commission - Directorate General for Maritime Affairs and Fisheries @ European Union, 2015-2016.

This file is part of the Integrated Fisheries Data Management (IFDM) Suite. The IFDM Suite is free software: you can redistribute it 
and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of 
the License, or any later version. The IFDM Suite is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; 
without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more 
details. You should have received a copy of the GNU General Public License along with the IFDM Suite. If not, see <http://www.gnu.org/licenses/>.

 */
package eu.europa.ec.fisheries.uvms.mdr.message.constants;

public class MdrMessageConstants {

    private MdrMessageConstants(){
        super();
    }


    public static final String MESSAGING_TYPE_STR   = "messagingType";
    public static final String DESTINATION_TYPE_STR = "destinationType";
    public static final String DESTINATION_STR      = "destination";

    public static final String CONNECTION_TYPE        = "javax.jms.MessageListener";
    public static final String DESTINATION_TYPE_QUEUE = "javax.jms.Queue";

    public static final String MDR_MESSAGE_IN_QUEUE = "java:/jms/queue/UVMSMdrEvent";
    public static final String MDR_MESSAGE_IN_QUEUE_NAME = "UVMSMdrEvent";

}