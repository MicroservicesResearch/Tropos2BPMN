import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Formatter;
import java.util.Stack;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.MessageFlow;
import org.camunda.bpm.model.bpmn.instance.Participant;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class transformer {
	
	public static void main(String[] args) {
		
		BpmnModelInstance model = null;
		
		Stack<String> actores = new Stack();
		Stack<String> objetivos = new Stack();
		Stack<String> actoresID = new Stack();
		Stack<String> objetivosID = new Stack();
		Stack<String> objetivosDep =  new Stack();
		Stack<String> dependencias = new Stack();
		Stack<String> inicioObjetivos = new Stack();
		Stack<String> finalObjetivos = new Stack();
		
		try {
			
			//Access the XML File
			File archivoXML = new File("user.dir\\archivo.cgm");
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = dbFactory.newDocumentBuilder();
			Document documentoXml = builder.parse(archivoXML);
			
			documentoXml.getDocumentElement().normalize();
			
			//Get the nodes corresponding to: Actors, goals and dependencies
			NodeList elements = documentoXml.getDocumentElement().getElementsByTagName("elements");
			
			for(int i=0; i < elements.getLength();i++) {
				Node element = elements.item(i);
				
				//Access each node individually
				if(element.getNodeType() == Node.ELEMENT_NODE) {
					Element nodeElement = (Element) element;
					
					//Actor nodes:
					if(nodeElement.getAttribute("xsi:type").equals("cgmtool:Goal")) {
						actores.addElement(nodeElement.getAttribute("name"));
						actoresID.addElement(nodeElement.getAttribute("outcomingRefinement"));
					}
					
					//Goal nodes:
					if(nodeElement.getAttribute("xsi:type").equals("cgmtool:Requriement")) {
						objetivos.addElement(nodeElement.getAttribute("name"));
						objetivosID.addElement(nodeElement.getAttribute("incomingRefinement"));
						
						//Check if the goal has dependencies on other goals: End Event
						if(nodeElement.hasAttribute("incomingContribution")) {
							String dependencia = nodeElement.getAttribute("incomingContribution");
							String depender = nodoDependencia(elements, dependencia);
							
							objetivosDep.addElement(nodeElement.getAttribute("name") + "-" + depender);
						}else {
							finalObjetivos.addElement(nodeElement.getAttribute("name"));
						}
						
						//Check if the goal has dependencies on other goals: Start Event
						if(nodeElement.hasAttribute("outgoingContribution")) {
							//****
						}else {
							inicioObjetivos.addElement(nodeElement.getAttribute("name"));
						}
					}
					
					//Dependency nodes: Actor goals
					if(nodeElement.getAttribute("xsi:type").equals("cgmtool:RefinementNode")) {
						String nameOut = nodeElement.getAttribute("outcomingRefinement");
						String nameIn = nodeElement.getAttribute("incomingRefinement");
						
						int out = 0;
						int in = 0;
						
						if(actoresID.contains(nameIn)) {
							out = actoresID.indexOf(nameIn);
						}
						if(objetivosID.contains(nameOut)) {
							in = objetivosID.indexOf(nameOut);
						}
						
						dependencias.add(actores.get(out) + "-" + objetivos.get(in));
					}
					
				}
			}
			
			System.out.println("Actores:");
			for(int i = 0; i < actores.size();i++) {
				System.out.println(actores.get(i));
			}
			System.out.println();
			
			System.out.println("Objetivos:");
			for(int i = 0; i < objetivos.size();i++) {
				System.out.println(objetivos.get(i));
			}
			System.out.println();
			
			System.out.println("Objetivos de actores:");
			for(int i = 0; i < dependencias.size();i++) {
				System.out.println(dependencias.get(i));
			}
			
			System.out.println();
			
			System.out.println("Dependencias entre objetivos:");
			for(int i = 0; i < objetivosDep.size();i++) {
				System.out.println(objetivosDep.get(i));
			}
			
			System.out.println();
			
			//Create the BPMN file
			Stack<String> objetivosEventos = crearDiagramaBPMN(actores, objetivos, dependencias, objetivosDep);
			
			//Access the BPMN file
			model=Bpmn.readModelFromFile(new File("user.dir\\diagrama.bpmn"));
			
			System.out.println(objetivosEventos);
			
			//Add flows between events
			for(int i=0; i<objetivosDep.size();i++) {
				String dep = objetivosDep.get(i);
				String[] depSeparada = dep.split("-");
				
				for(int i2=0;i2<objetivosEventos.size();i2++) {
					String intercambio = objetivosEventos.get(i2);
					String[] intercambioSeparado = intercambio.split("-");
					
					if(depSeparada[0].equals(intercambioSeparado[0]) && (intercambioSeparado[1].equals("End Event") || intercambioSeparado[1].equals("Throw Event"))) {
						Collection<MessageFlow> messageFlows = model.getModelElementsByType(MessageFlow.class);
						for(MessageFlow message:messageFlows) {
							message.setAttributeValue("sourceRef", intercambioSeparado[2]);
						}
					}
					if(depSeparada[1].equals(intercambioSeparado[0]) && (intercambioSeparado[1].equals("Start Event") || intercambioSeparado[1].equals("Catch Event"))) {
						Collection<MessageFlow> messageFlows = model.getModelElementsByType(MessageFlow.class);
						for(MessageFlow message:messageFlows) {
							message.setAttributeValue("targetRef", intercambioSeparado[2]);
						}
					}
				}
			}
						
			//Save modified file
	        File fichero=new File("user.dir\\diagramModified.bpmn");
			PrintWriter writer = new PrintWriter(fichero, "UTF-8");
			writer.print(Bpmn.convertToString(model));
			writer.close();
			
		} catch(IOException | ParserConfigurationException | SAXException e) {
			System.err.println("Error: " + e.getMessage());
			
		}
	}
	
	static String nodoDependencia(NodeList elements, String dependencia) {
		
		String resultado = null;
		
		for(int i=0; i<elements.getLength(); i++) {
			Node element = elements.item(i);
			
			//Access each node individually
			if(element.getNodeType() == Node.ELEMENT_NODE) {
				Element nodeElement = (Element) element;
				
				//Access goals
				if(nodeElement.getAttribute("xsi:type").equals("cgmtool:Requriement")) {
					
					//Check if the target has dependencies on other targets: End Event
					if(nodeElement.hasAttribute("outgoingContribution")) {
						if(nodeElement.getAttribute("outgoingContribution").equals(dependencia)) {
							resultado = nodeElement.getAttribute("name");
						}
					}
				}
			}
		}
		
		return resultado;
	}
	
	public static Stack<String> crearDiagramaBPMN(Stack<String> actores, Stack<String> objetivos, Stack<String> dependencias, Stack<String> objetivosDep) {
		
		Formatter diagrama = null;
		
		Stack<String> participantID = new Stack();
		Stack<String> procesoID = new Stack();
		Stack<String> inicialID =  new Stack();
		Stack<String> subprocesoID =  new Stack();
		Stack<String> finalID =  new Stack();
		Stack<String> mensajeID = new Stack();
		Stack<String> flowID = new Stack();
		Stack<String> eventosID = new Stack();
		Stack<String> objetivosEventos =  new Stack();
		
		int event = 0, process = 0;
		
		try {
			
			diagrama = new Formatter("user.dir\\diagrama.bpmn");
			
			diagrama.format("%s", "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n"
					+ "<bpmn:definitions xmlns:bpmn=\"http://www.omg.org/spec/BPMN/20100524/MODEL\" xmlns:bpmndi=\"http://www.omg.org/spec/BPMN/20100524/DI\" xmlns:dc=\"http://www.omg.org/spec/DD/20100524/DC\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:di=\"http://www.omg.org/spec/DD/20100524/DI\" id=\"Definitions_09jeo44\" targetNamespace=\"http://bpmn.io/schema/bpmn\" exporter=\"Camunda Modeler\" exporterVersion=\"5.6.0\">\r\n"
					+ "  <bpmn:collaboration id=\"Collaboration_1pxz3i0\" isClosed=\"false\">\r\n");
			
			
			//Pools
			for(int i=0; i<actores.size();i++) {
				diagrama.format("%s", "   <bpmn:participant id=\"Participant_0akjyc9" + i +"\" name=\"" + actores.get(i) + "\" processRef=\"Process_0mrnjvg" + i + "\" />\r\n");
				participantID.add("Participant_0akjyc9" + i);
				procesoID.add("Process_0mrnjvg" + i );
			}
			
			//Dependencies between goals
			for(int i=0; i<objetivosDep.size();i++) {
				diagrama.format("%s", "<bpmn:messageFlow id=\"Flow_0ljxz4d"+i+"\" sourceRef=\"source\" targetRef=\"target\" />");
				eventosID.add("Flow_0ljxz4d"+i);
			}
			
			diagrama.format("%s", "  </bpmn:collaboration>\r\n");
			
			//Process
			for(int i=0;i<actores.size();i++) {	
				
				int aux = 1;
				int numObjetivos = contarObjetivos(actores.get(i), dependencias);
				
				//Collapsed subprocess
				for(int i2 = 0; i2 < dependencias.size(); i2++) {
					if(dependencias.get(i2).contains(actores.get(i))) {
						
						String incomingFlow = "";
						String outgoingFlow = "";
						String pre = "";
						String post = "";
						
						String dependencia = dependencias.get(i2).toString();
						String[] depPartida = dependencia.split("-");
						
						if(aux == 1) {
							//Start Events
							diagrama.format("%s", "  <bpmn:process id=\""+ procesoID.get(i) + "\" processType=\"None\" isClosed=\"false\" isExecutable=\"false\">\r\n");
							diagrama.format("%s", "    <bpmn:startEvent id=\"Event_1q92mio" + event + "\">\r\n"
									+ "    <bpmn:outgoing>Flow_0ev7qx8"+ event +"</bpmn:outgoing>\r\n"
									+ "    <bpmn:messageEventDefinition id=\"MessageEventDefinition_0jr8is0" + event + "\"/>\r\n"
									+ "    </bpmn:startEvent>\r\n");
							
							inicialID.add("Event_1q92mio" + event);
							mensajeID.add("MessageEventDefinition_0jr8is0" + event);
							incomingFlow = "Flow_0ev7qx8"+ event;
							pre = "Event_1q92mio" + event;
							flowID.add(incomingFlow);
							objetivosEventos.add(depPartida[1] + "-" + "Start Event" + "-" + "Event_1q92mio" + event);
						}
						else {
							//Intermediate Catching Events
							diagrama.format("%s", "  <bpmn:process id=\""+ procesoID.get(i) + "\" processType=\"None\" isClosed=\"false\" isExecutable=\"false\">\r\n");
							diagrama.format("%s", "    <bpmn:intermediateCatchEvent id=\"Event_1q92mio" + event + "\">\r\n"
									+ "    <bpmn:outgoing>Flow_0ev7qx8"+ event +"</bpmn:outgoing>\r\n"
									+ "    <bpmn:messageEventDefinition id=\"MessageEventDefinition_0jr8is0" + event + "\"/>\r\n"
									+ "    </bpmn:intermediateCatchEvent>\r\n");
							
							inicialID.add("Event_1q92mio" + event);
							mensajeID.add("MessageEventDefinition_0jr8is0" + event);
							incomingFlow = "Flow_0ev7qx8"+ event;
							pre = "Event_1q92mio" + event;
							flowID.add(incomingFlow);
							objetivosEventos.add(depPartida[1] + "-" + "Catch Event" + "-" + "Event_1q92mio" + event);
						}
						
						event++;
						
						if(aux == numObjetivos) {
							//End Events
							diagrama.format("%s", "    <bpmn:endEvent id=\"Event_0b70bw4" + event + "\">\r\n"
									+ "     <bpmn:incoming>Flow_0ev7qx8"+event+"</bpmn:incoming>\r\n"
									+ "    <bpmn:messageEventDefinition id=\"MessageEventDefinition_0jr8is0" + event + "\"/>\r\n"
									+ "    </bpmn:endEvent>\r\n");
							
							finalID.add("Event_0b70bw4" + event);
							mensajeID.add("MessageEventDefinition_0jr8is0" + event);
							outgoingFlow = "Flow_0ev7qx8"+ event;
							post = "Event_0b70bw4" + event;
							flowID.add(outgoingFlow);
							objetivosEventos.add(depPartida[1] + "-" + "End Event" + "-" + "Event_0b70bw4" + event);
						}
						else {
							//Intermediate Throwing Events
							diagrama.format("%s", "    <bpmn:intermediateThrowEvent id=\"Event_0b70bw4" + event + "\">\r\n"
									+ "     <bpmn:incoming>Flow_0ev7qx8"+event+"</bpmn:incoming>\r\n"
									+ "    <bpmn:messageEventDefinition id=\"MessageEventDefinition_0jr8is0" + event + "\"/>\r\n"
									+ "    </bpmn:intermediateThrowEvent>\r\n");
							
							finalID.add("Event_0b70bw4" + event);
							mensajeID.add("MessageEventDefinition_0jr8is0" + event);
							outgoingFlow = "Flow_0ev7qx8"+ event;
							post = "Event_0b70bw4" + event;
							flowID.add(outgoingFlow);
							objetivosEventos.add(depPartida[1] + "-" + "Throw Event" + "-" + "Event_0b70bw4" + event);
						}
						
						event++;
						
						diagrama.format("%s", "    <bpmn:subProcess id=\"Activity_0njxs79"+ i2 + "\" name=\"" + depPartida[1] + "\">\r\n");
						diagrama.format("%s", "      <bpmn:incoming>"+incomingFlow+"</bpmn:incoming>\r\n");
						diagrama.format("%s", "      <bpmn:outgoing>"+outgoingFlow+"</bpmn:outgoing>\r\n");
						diagrama.format("%s", "    </bpmn:subProcess>\r\n");
						
						subprocesoID.add("Activity_0njxs79"+ i2);
					
						aux++;
						
						diagrama.format("%s", "    <bpmn:sequenceFlow id=\""+incomingFlow+"\" sourceRef=\""+pre+"\" targetRef=\"Activity_0njxs79"+i2+"\" />\r\n"
								+ "    <bpmn:sequenceFlow id=\""+outgoingFlow+"\" sourceRef=\"Activity_0njxs79"+i2+"\" targetRef=\""+post+"\" />");
					}
				}
				
				//End Process
				diagrama.format("%s", "  </bpmn:process>\r\n");
				
			}
			
			//Representation
			
			diagrama.format("%s", "  <bpmndi:BPMNDiagram id=\"BPMNDiagram_1\">\r\n"
					+ "    <bpmndi:BPMNPlane id=\"BPMNPlane_1\" bpmnElement=\"Collaboration_1pxz3i0\">\r\n");
			
			int x = 160;
			int y = 80;
			int width = 600;
			int height = 250;
			
			//Pools
			for(int i=0; i<participantID.size();i++) {
				diagrama.format("%s", "      <bpmndi:BPMNShape id=\""+ participantID.get(i) +"_di\" bpmnElement=\""+ participantID.get(i) +"\" isHorizontal=\"true\">\r\n"
						+ "        <dc:Bounds x=\""+x+"\" y=\""+y+"\" width=\""+width+"\" height=\""+height+"\" />\r\n"
						+ "        <bpmndi:BPMNLabel />\r\n"
						+ "      </bpmndi:BPMNShape>\r\n");
				y = y + 300;
			}
			
			x = 212;
			y = 182;
			width = 36;
			height = 36;
			
			//Start Events
			for(int i=0; i<inicialID.size();i++) {
				diagrama.format("%s", "      <bpmndi:BPMNShape id=\""+ inicialID.get(i) +"_di\" bpmnElement=\""+ inicialID.get(i) +"\">\r\n"
						+ "        <dc:Bounds x=\""+x+"\" y=\""+y+"\" width=\""+width+"\" height=\""+height+"\" />\r\n"
						+ "      </bpmndi:BPMNShape>\r\n");
				y = y + 300;
			}
			
			x = 310;
			y = 160;
			width = 100;
			height = 80;
			
			//Subprocess
			for(int i=0;i<subprocesoID.size();i++) {
				diagrama.format("%s", "      <bpmndi:BPMNShape id=\""+ subprocesoID.get(i) +"_di\" bpmnElement=\""+ subprocesoID.get(i) +"\" isExpanded=\"false\">\r\n"
						+ "        <dc:Bounds x=\""+x+"\" y=\""+y+"\" width=\""+width+"\" height=\""+height+"\" />\r\n"
						+ "        <bpmndi:BPMNLabel />\r\n"
						+ "      </bpmndi:BPMNShape>\r\n");
				y = y + 300;
			}
			
			x = 472;
			y = 182;
			width = 36;
			height = 36;
			
			//End Events
			for(int i=0; i<finalID.size();i++) {
				diagrama.format("%s", "      <bpmndi:BPMNShape id=\""+ finalID.get(i) +"_di\" bpmnElement=\""+ finalID.get(i) +"\">\r\n"
						+ "        <dc:Bounds x=\""+x+"\" y=\""+y+"\" width=\""+width+"\" height=\""+height+"\" />\r\n"
						+ "      </bpmndi:BPMNShape>\r\n");
				y = y + 300;
			}
			
			//Flows
			for(int i=0; i<flowID.size();i++) {
				diagrama.format("%s", "      <bpmndi:BPMNEdge id=\""+flowID.get(i)+"_di\" bpmnElement=\""+flowID.get(i)+"\">\r\n"
						+ "        <di:waypoint x=\"258\" y=\"200\" />\r\n"
						+ "        <di:waypoint x=\"340\" y=\"200\" />\r\n"
						+ "      </bpmndi:BPMNEdge>\r\n");
			}
			
			//Flows between actors
			for(int i=0; i<eventosID.size();i++) {
				diagrama.format("%s", "      <bpmndi:BPMNEdge id=\""+eventosID.get(i)+"_di\" bpmnElement=\""+eventosID.get(i)+"\">\r\n"
						+ "        <di:waypoint x=\"530\" y=\"218\" />\r\n"
						+ "        <di:waypoint x=\"530\" y=\"345\" />\r\n"
						+ "        <di:waypoint x=\"230\" y=\"345\" />\r\n"
						+ "        <di:waypoint x=\"230\" y=\"472\" />\r\n"
						+ "      </bpmndi:BPMNEdge>");
			}
			
			diagrama.format("%s", "    </bpmndi:BPMNPlane>\r\n"
					+ "  </bpmndi:BPMNDiagram>\r\n");
			
			//End
			diagrama.format("%s", "</bpmn:definitions>");
			
		}catch(Exception e) {
			System.out.println("Error:");
		}finally {
			diagrama.close();
		}
		
		return objetivosEventos;
	}
	
	public static int contarObjetivos(String actor, Stack<String> dependencias) {
		int res = 0;
		
		for(int i=0; i<dependencias.size();i++) {
			if(dependencias.get(i).contains(actor)) {
				res++;
			}
		}
		return res;
	}
}
