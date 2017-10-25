package org.waziup.socialapi.core;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.enterprise.inject.Model;
import javax.inject.Inject;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import org.bson.Document;
import org.waziup.socialapi.ejb.NotificationBean;
import twitter4j.DirectMessage;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.conf.ConfigurationBuilder;

/**
 *
 * @author Drabo Constantin
 *
 */
@Model
@Path("socials")
public class Socials implements Serializable {

    ConfigurationBuilder config;
    TwitterFactory tf;
    Twitter twitter;
    DirectMessage tweet;
    @Inject
    NotificationBean notificationbean;

    /**
     * Constructor
     */
    public Socials() {
        ResourceBundle waziupNotificationBundle = ResourceBundle.getBundle("parameters");
        config = new ConfigurationBuilder();
        config.setDebugEnabled(true)
                .setOAuthConsumerKey(waziupNotificationBundle.getString("OAuthConsumerKey"))
                .setOAuthConsumerSecret(waziupNotificationBundle.getString("OAuthConsumerSecret"))
                .setOAuthAccessToken(waziupNotificationBundle.getString("OAuthAccessToken"))
                .setOAuthAccessTokenSecret(waziupNotificationBundle.getString("OAuthAccessTokenSecret"));
        tf = new TwitterFactory(config.build());
        twitter = tf.getInstance();
    }

    /**
     *
     * @param doc
     * @return
     */
    @POST
    @Produces("application/json")
    public Response sendNotification(Document doc) {

        String channel = null;

        if (doc != null) {
            channel = doc.getString("channel");
            switch (channel) {
                case "facebook":
                    sendFacebookMessage(doc.getString("user_id"), doc.getString("message"));
                    break;
                case "twitter":
                    sendTwitterMessage(doc.getString("user_id"), doc.getString("message"));
                    break;
                default:
                    sendTwitterMessage(doc.getString("user_id"), doc.getString("message"));
            }

        } else {
            channel = "Invalid document";
        }

        return Response.accepted(channel).build();
    }
    

    /**
     *
     * @return
     */
    @GET
    @Produces("application/json")
    public List<Document> getMessageHistory() {
        return notificationbean.queryNotification();
    }

    /**
     *
     * @return
     */
    @DELETE
    @Produces("application/json")
    @Path("/{id}")
    public Response deleteOneNotification(@PathParam("id") String id) {
        notificationbean.deleteOneNotification(id);
        return Response.ok(id + "  deleted from notification").build();
    }

    /**
     *
     * @return
     */
    @DELETE
    @Produces("application/json")
    public Response deleteAllNotification() {
        notificationbean.deleteAllNotification();
        return Response.ok("All the notifications deleted").build();
    }

    /**
     *
     * @param recipient
     * @param message
     */
    public void sendFacebookMessage(String recipient, String message) {

    }

    /**
     *
     * @param recipient
     * @param message
     */
    public void sendTwitterMessage(String recipient, String message) {
        try {
            tweet = twitter.sendDirectMessage(recipient, message);
            Document notification = new Document("user_id", recipient).append("channel", "twitter").append("message", message).append("status", "Delivered").append("inserttime", LocalDateTime.now().toString());
            notificationbean.createNotification(notification);
        } catch (TwitterException ex) {
            Document notificationfailure = new Document("user_id", recipient).append("channel", "twitter").append("message", message).append("status", "Not delivered").append("inserttime", LocalDateTime.now().toString());
            notificationbean.createNotification(notificationfailure);
            Logger.getLogger(Socials.class.getName()).log(Level.SEVERE, tweet.getId() + " did not deliver", ex);
        } catch (WebApplicationException webex) {
            Logger.getLogger(Socials.class.getName()).log(Level.INFO, webex.getMessage());
        } finally {
            Logger.getLogger(Socials.class.getName()).log(Level.INFO, "Message delivered by {0} to {1}", new Object[]{tweet.getSenderScreenName(), tweet.getRecipientScreenName()});

        }
    }

}
