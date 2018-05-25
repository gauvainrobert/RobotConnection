package rc;

import java.util.ArrayList;
import java.util.List;

import lejos.pc.comm.NXTComm;
import lejos.pc.comm.NXTCommException;
import lejos.pc.comm.NXTCommFactory;
import lejos.pc.comm.NXTInfo;
import robotambulance.course.Course;
import robotambulance.course.Position;
import robotambulance.course.Vertice;

public class Main {
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {

			NXTInfo[] nxts = {

//					new NXTInfo(NXTCommFactory.BLUETOOTH, "Gurren Lagann",
//							"00165318EB71"),
					new NXTInfo(NXTCommFactory.BLUETOOTH, "Hydra",
							"0016531C24E7"),
					new NXTInfo(NXTCommFactory.BLUETOOTH, "NXT",
					"0016531C15FC"),

					 };

			ArrayList<Robot> connections = new ArrayList<>(
					nxts.length);

			Course course = Course.compet2();
			
			List<Position> positions = new ArrayList<>();
			
			
			//positions.add(new Position(new Vertice("a2"), new Vertice("h3"), 270.f-15.f));
			positions.add(new Position(new Vertice("a2"), new Vertice("h3"), 0.f));
			positions.add(new Position(new Vertice("f2"), new Vertice("d3"), 0.f));
			int index = 0;
			for (NXTInfo nxt : nxts) {
				connections.add(new Robot(nxt,connections,positions.get(index),course,index));
				index++;
			}

			for (Robot connection : connections) {
				NXTComm nxtComm = NXTCommFactory
						.createNXTComm(NXTCommFactory.BLUETOOTH);
				connection.connect(nxtComm);
			}

			ArrayList<Thread> threads = new ArrayList<>(nxts.length);

			for (Robot connection : connections) {
				threads.add(new Thread(connection));
			}

			for (Thread thread : threads) {
				thread.start();
			}

			for (Thread thread : threads) {
				try {
					thread.join();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			
			
			
			

		} catch (NXTCommException e) {
			e.printStackTrace();
		}

	}

	
}
