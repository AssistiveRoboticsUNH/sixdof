/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mywebsocket;

/**
 *
 * @author FridayLab
 */
public interface WebsocketInterface {

    public void onopen(String remote);

    public void onmessage(String msg);

    public void onclose();
}
