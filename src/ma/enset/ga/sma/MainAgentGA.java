package ma.enset.ga.sma;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.SequentialBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import ma.enset.ga.sequencial.GAUtils;
import ma.enset.ga.sequencial.Individual;

import java.util.*;

public class MainAgentGA extends Agent {
    List<AgentFitness> agentsFitness=new ArrayList<>();
    Random rnd = new Random();
    @Override
    protected void setup() {
        DFAgentDescription dfAgentDescription=new DFAgentDescription();
        ServiceDescription serviceDescription=new ServiceDescription();
        serviceDescription.setType("ga");
        dfAgentDescription.addServices(serviceDescription);
        try {
            DFAgentDescription[] agentsDescriptions = DFService.search(this, dfAgentDescription);
            System.out.println(agentsDescriptions.length);
            for (DFAgentDescription dfAD:agentsDescriptions) {
                agentsFitness.add(new AgentFitness(dfAD.getName(),0));
            }
        } catch (FIPAException e) {
            e.printStackTrace();
        }
        calculateFintness();
        SequentialBehaviour sq = new SequentialBehaviour();
        sq.addSubBehaviour(new Behaviour() {
            int cpt=0;
            @Override
            public void action() {
                ACLMessage receivedMSG = receive();
                if (receivedMSG!=null){
                    cpt++;
                    System.out.println(cpt);
                    int fintess=Integer.parseInt(receivedMSG.getContent());
                    AID sender=receivedMSG.getSender();
                    setAgentFintess(sender,fintess);
                    if(cpt==GAUtils.POPULATION_SIZE){
                        Collections.sort(agentsFitness,Collections.reverseOrder());
                        showPopulation();
                    }
                }else {
                    block();
                }
            }

            @Override
            public boolean done() {
                return cpt==GAUtils.POPULATION_SIZE;
            }

        });
    //=========================Processs===============================
    sq.addSubBehaviour(new Behaviour() {
        int it =0;
        AgentFitness agnet1;
        AgentFitness agent2;
        @Override
        public void action() {
            selection();
            crossover();
            Collections.sort(agentsFitness,Collections.reverseOrder());

            System.out.println("agentsFitness.get(0) = " + agentsFitness.get(0).getFitness());
            System.out.println("agentsFitness.get(0).getAid() = " + agentsFitness.get(0).getAid());

        }
        private void selection(){
            System.out.println("=========selection=========");
            agnet1 = agentsFitness.get(0);
            agent2 = agentsFitness.get(1);
            ACLMessage aclMessage = new ACLMessage(ACLMessage.REQUEST);
            aclMessage.setConversationId("chromosome");
            aclMessage.addReceiver(agnet1.getAid());
            aclMessage.addReceiver(agent2.getAid());
            send(aclMessage);
            ++it;

        }
        private void crossover(){
            ACLMessage aclMessage1 = blockingReceive();
            ACLMessage aclMessage2 = blockingReceive();

            int pointCroisment=rnd.nextInt(GAUtils.MAX_FITNESS-1)+1;
            pointCroisment++;
            char []  chromP1 = aclMessage1.getContent().toCharArray();
            char[]  chromP2= aclMessage2.getContent().toCharArray();
            char[] chromOfSone1 = new char[GAUtils.MAX_FITNESS];
            char[] chromOfSone2 = new char[GAUtils.MAX_FITNESS];
            for (int i=0;i<chromP1.length;i++) {

                chromOfSone1[i]=chromP1[i];
                chromOfSone2[i]=chromP2[i];
            }
            for (int i=0;i<pointCroisment;i++) {
                chromOfSone1[i]=chromP2[i];
                chromOfSone2[i]=chromP1[i];
            }
            //===================================================

            ACLMessage message1 = new ACLMessage(ACLMessage.INFORM);
            message1.setConversationId("change chromosome");
            message1.setContent(new String(chromOfSone1));
            message1.addReceiver(agentsFitness.get(GAUtils.POPULATION_SIZE-2).getAid());
            send(message1);

            /** ============================**/


            ACLMessage message2 = new ACLMessage(ACLMessage.INFORM);
            message2.setConversationId("change chromosome");
            message2.setContent(new String(chromOfSone2));
            message2.addReceiver(agentsFitness.get(GAUtils.POPULATION_SIZE-1).getAid());
            send(message2);
            /** =====================**/
            ACLMessage recivedAclMessage1  = blockingReceive();
            ACLMessage recivedAclMessage2 = blockingReceive();
            setAgentFintess(recivedAclMessage1.getSender(),Integer.parseInt(recivedAclMessage1.getContent()));
            setAgentFintess(recivedAclMessage2.getSender(),Integer.parseInt(recivedAclMessage2.getContent()));


        }

        @Override
        public boolean done() {
              return  GAUtils.MAX_IT==it || agentsFitness.get(0).getFitness()==GAUtils.MAX_FITNESS;
         }


    });
    //========================================================
    addBehaviour(sq);

    }

    private void calculateFintness(){
        ACLMessage message=new ACLMessage(ACLMessage.REQUEST);

        for (AgentFitness agf:agentsFitness) {
            message.addReceiver(agf.getAid());
        }
        message.setConversationId("fitness");
        send(message);

    }

    private void setAgentFintess(AID aid,int fitness){
            for (int i=0;i<GAUtils.POPULATION_SIZE;i++){
                if(agentsFitness.get(i).getAid().equals(aid)){
                    agentsFitness.get(i).setFitness(fitness);
                    System.out.println(fitness+"=:="+agentsFitness.get(i).getFitness());
                    break;
                }
            }
    }
    private void showPopulation(){
        for (AgentFitness agentFitness:agentsFitness) {
            System.out.println(agentFitness.getAid().getName()+" "+agentFitness.getFitness());
        }
    }

}