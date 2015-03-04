/****** SALSA LANGUAGE IMPORTS ******/
import salsa_lite.common.DeepCopy;
import salsa_lite.runtime.LocalActorRegistry;
import salsa_lite.runtime.Hashing;
import salsa_lite.runtime.Acknowledgement;
import salsa_lite.runtime.SynchronousMailboxStage;
import salsa_lite.runtime.Actor;
import salsa_lite.runtime.Message;
import salsa_lite.runtime.RemoteActor;
import salsa_lite.runtime.MobileActor;
import salsa_lite.runtime.StageService;
import salsa_lite.runtime.TransportService;
import salsa_lite.runtime.language.Director;
import salsa_lite.runtime.language.JoinDirector;
import salsa_lite.runtime.language.MessageDirector;
import salsa_lite.runtime.language.ContinuationDirector;
import salsa_lite.runtime.language.TokenDirector;

import salsa_lite.runtime.language.exceptions.RemoteMessageException;
import salsa_lite.runtime.language.exceptions.TokenPassException;
import salsa_lite.runtime.language.exceptions.MessageHandlerNotFoundException;
import salsa_lite.runtime.language.exceptions.ConstructorNotFoundException;

/****** END SALSA LANGUAGE IMPORTS ******/

import salsa_lite.runtime.language.JoinDirector;
import salsa_lite.runtime.StageService;

public class ThreadRing extends salsa_lite.runtime.Actor implements java.io.Serializable {


    public Object writeReplace() throws java.io.ObjectStreamException {
        int hashCode = Hashing.getHashCodeFor(this.hashCode(), TransportService.getHost(), TransportService.getPort());
        synchronized (LocalActorRegistry.getLock(hashCode)) {
            LocalActorRegistry.addEntry(hashCode, this);
        }
        return new SerializedThreadRing( this.hashCode(), TransportService.getHost(), TransportService.getPort() );
    }

    public static class RemoteReference extends ThreadRing {
        private int hashCode;
        private String host;
        private int port;
        RemoteReference(int hashCode, String host, int port) { this.hashCode = hashCode; this.host = host; this.port = port; }

        public Object invokeMessage(int messageId, Object[] arguments) throws RemoteMessageException, TokenPassException, MessageHandlerNotFoundException {
            TransportService.sendMessageToRemote(host, port, this.getStage().message);
            throw new RemoteMessageException();
        }

        public void invokeConstructor(int messageId, Object[] arguments) throws RemoteMessageException, ConstructorNotFoundException {
            TransportService.sendMessageToRemote(host, port, this.getStage().message);
            throw new RemoteMessageException();
        }

        public Object writeReplace() throws java.io.ObjectStreamException {
            return new SerializedThreadRing( hashCode, host, port);
        }
    }

    public static class SerializedThreadRing implements java.io.Serializable {
        int hashCode;
        String host;
        int port;

        SerializedThreadRing(int hashCode, String host, int port) { this.hashCode = hashCode; this.host = host; this.port = port; }

        public Object readResolve() throws java.io.ObjectStreamException {
            int hashCode = Hashing.getHashCodeFor(this.hashCode, this.host, this.port);
            synchronized (LocalActorRegistry.getLock(hashCode)) {
                ThreadRing actor = (ThreadRing)LocalActorRegistry.getEntry(hashCode);
                if (actor == null) {
                    RemoteReference remoteReference = new RemoteReference(this.hashCode, this.host, this.port);
                    LocalActorRegistry.addEntry(hashCode, remoteReference);
                    return remoteReference;
                } else {
                    return actor;
                }
            }
        }
    }

    public String getMessageInformation(int messageId) {
    	switch (messageId) {
    		case 0: return "java.lang.String [salsa_lite.runtime.Actor].toString()";
    		case 1: return "int [salsa_lite.runtime.Actor].hashCode()";
    		case 2: return "int [salsa_lite.runtime.Actor].getStageId()";
    		case 3: return "ack [ThreadRing].setNextThread(ThreadRing)";
    		case 4: return "ack [ThreadRing].forwardMessage(int)";
    	}
    	return "No message with specified id.";
    }

    public ThreadRing() { super(); }
    public ThreadRing(int stage_id) { super(stage_id); }

    ThreadRing next;
    int id;


    public Object invokeMessage(int messageId, Object[] arguments) throws RemoteMessageException, TokenPassException, MessageHandlerNotFoundException {
        switch(messageId) {
            case 0: return toString();
            case 1: return hashCode();
            case 2: return getStageId();
            case 3: setNextThread( (ThreadRing)arguments[0] ); return null;
            case 4: forwardMessage( (Integer)arguments[0] ); return null;
            default: throw new MessageHandlerNotFoundException(messageId, arguments);
        }
    }

    public void invokeConstructor(int messageId, Object[] arguments) throws RemoteMessageException, ConstructorNotFoundException {
        switch(messageId) {
            case 0: construct( (Integer)arguments[0] ); return;
            case 1: construct( (String[])arguments[0] ); return;
            default: throw new ConstructorNotFoundException(messageId, arguments);
        }
    }

    public void construct(int id) {
        ((ThreadRing)this).id = id;
    }

    public void construct(String[] args) {
        if (args.length != 2) {
            System.out.println("Usage: java ThreadRing <threadCount> <hopCount>");
            return;
        }
        
        int threadCount = Integer.parseInt(args[0]);
        int hopCount = Integer.parseInt(args[1]);
        ThreadRing first = ThreadRing.construct(0, new Object[]{1});
        JoinDirector jd = JoinDirector.construct(0, null);
        ThreadRing next = null;
        ThreadRing previous = first;
        for (int i = 1; i < threadCount; i++) {
            next = ThreadRing.construct(0, new Object[]{i + 1}, -1);
            ContinuationDirector continuation_token = StageService.sendContinuationMessage(previous, 3 /*setNextThread*/, new Object[]{next});
            StageService.sendMessage(jd, 3 /*join*/, null, continuation_token);
            previous = next;
        }

        ContinuationDirector continuation_token = StageService.sendContinuationMessage(next, 3 /*setNextThread*/, new Object[]{first});
        StageService.sendMessage(jd, 3 /*join*/, null, continuation_token);
        continuation_token = StageService.sendContinuationMessage(jd, 4 /*resolveAfter*/, new Object[]{threadCount});
        StageService.sendMessage(first, 4 /*forwardMessage*/, new Object[]{hopCount}, continuation_token);
    }



    public void setNextThread(ThreadRing next) {
        ((ThreadRing)this).next = next;
    }

    public void forwardMessage(int value) {
        if (value == 0) {
            System.out.println(id);
            System.exit(0);
        }
        else {
            value--;
            StageService.sendMessage(next, 4 /*forwardMessage*/, new Object[]{value});
        }

    }


    public static void main(String[] arguments) {
        ThreadRing.construct(1, new Object[]{arguments});
    }

    public static ThreadRing construct(int constructor_id, Object[] arguments) {
        ThreadRing actor = new ThreadRing();
        StageService.sendMessage(new Message(Message.CONSTRUCT_MESSAGE, actor, constructor_id, arguments));
        return actor;
    }

    public static ThreadRing construct(int constructor_id, Object[] arguments, int target_stage_id) {
        ThreadRing actor = new ThreadRing(target_stage_id);
        actor.getStage().putMessageInMailbox(new Message(Message.CONSTRUCT_MESSAGE, actor, constructor_id, arguments));
        return actor;
    }

    public static TokenDirector construct(int constructor_id, Object[] arguments, int[] token_positions) {
        ThreadRing actor = new ThreadRing();
        TokenDirector output_continuation = TokenDirector.construct(0 /*construct()*/, null);
        Message input_message = new Message(Message.CONSTRUCT_CONTINUATION_MESSAGE, actor, constructor_id, arguments, output_continuation);
        MessageDirector md = MessageDirector.construct(3, new Object[]{input_message, arguments, token_positions});
        return output_continuation;
    }

    public static TokenDirector construct(int constructor_id, Object[] arguments, int[] token_positions, int target_stage_id) {
        ThreadRing actor = new ThreadRing(target_stage_id);
        TokenDirector output_continuation = TokenDirector.construct(0 /*construct()*/, null, target_stage_id);
        Message input_message = new Message(Message.CONSTRUCT_CONTINUATION_MESSAGE, actor, constructor_id, arguments, output_continuation);
        MessageDirector md = MessageDirector.construct(3, new Object[]{input_message, arguments, token_positions}, target_stage_id);
        return output_continuation;
    }

}
