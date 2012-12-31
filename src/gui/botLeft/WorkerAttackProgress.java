package gui.botLeft;

import gui.botLeft.PanelAttackProgress;
import attacks.AttackAuthorisation;
import attacks.AttackI;
import attacks.AttackMain;
import attacks.AttackPersistentXss;
import attacks.AttackSql;
import attacks.AttackXss;
import java.util.LinkedList;
import java.util.List;
import javax.swing.SwingWorker;
import model.AttackTypeData;
import model.SentinelHttpMessage;
import model.SentinelHttpParam;

/**
 *
 * @author unreal
 */
public class WorkerAttackProgress extends SwingWorker<LinkedList<SentinelHttpMessage>, SentinelHttpMessage> {

    private SentinelHttpMessage origHttpMessage;
    private LinkedList<SentinelHttpParam> attackHttpParams;
    private PanelAttackProgress panelProgress;
    private boolean followRedirect = false;
    private String mainSessionName;

    public WorkerAttackProgress(LinkedList<SentinelHttpParam> attackHttpParams, 
            SentinelHttpMessage origHttpMessage, 
            PanelAttackProgress panelProgress, 
            boolean followRedirect,
            String mainSessionName) 
    {
        this.origHttpMessage = origHttpMessage;
        this.attackHttpParams = attackHttpParams;
        this.panelProgress = panelProgress;
        this.followRedirect = followRedirect;
        this.mainSessionName = mainSessionName;
    }
    private boolean isCanceled = false;

    @Override
    protected void process(List<SentinelHttpMessage> strings) {
        for (SentinelHttpMessage s : strings) {
            panelProgress.addText("Did send: "
                    + s.getReq().getChangeParam().getName()
                    + "="
                    + s.getReq().getChangeParam().getValue());
            panelProgress.addAttackMessage(s);
        }
    }

    @Override
    protected void done() {
        panelProgress.done();
    }

    private void performAttack(AttackI attack, LinkedList<SentinelHttpMessage> httpMessages) {
        SentinelHttpMessage attackMessage = null;
        boolean goon = true;

        while (goon) {
            goon = attack.performNextAttack();
            attackMessage = attack.getLastAttackMessage();

            if (attackMessage != null) {
                httpMessages.add(attackMessage);
                publish(attackMessage);
            }
        }
    }

    @Override
    protected LinkedList<SentinelHttpMessage> doInBackground() throws Exception {
        LinkedList<SentinelHttpMessage> httpMessages = new LinkedList<SentinelHttpMessage>();

        for (SentinelHttpParam attackHttpParam : attackHttpParams) {
            if (isCanceled) {
                break;
            }
            
//            origHttpMessage.getReq().setOrigParam(attackHttpParam);

            // XSS
            AttackTypeData xssAttackData = attackHttpParam.getAttackType(AttackMain.AttackTypes.XSS);
            if (xssAttackData != null && xssAttackData.isActive()) {
                AttackI attack = new AttackXss(origHttpMessage, mainSessionName, followRedirect, attackHttpParam);
                performAttack(attack, httpMessages);
            }
            
            // pXSS
            AttackTypeData pxssAttackData = attackHttpParam.getAttackType(AttackMain.AttackTypes.pXSS);
            if (pxssAttackData != null && pxssAttackData.isActive()) {
                AttackI attack = new AttackPersistentXss(origHttpMessage, mainSessionName, followRedirect, attackHttpParam);
                performAttack(attack, httpMessages);
            }

            // SQL
            AttackTypeData sqlAttackData = attackHttpParam.getAttackType(AttackMain.AttackTypes.SQL);
            if (sqlAttackData != null && sqlAttackData.isActive()) {
                AttackI attack = new AttackSql(origHttpMessage, mainSessionName, followRedirect, attackHttpParam);
                performAttack(attack, httpMessages);
            }

            // Authorisation
            AttackTypeData authAttackData = attackHttpParam.getAttackType(AttackMain.AttackTypes.AUTHORISATION);
            if (authAttackData != null && authAttackData.isActive()) {
                AttackI attack = new AttackAuthorisation(origHttpMessage, mainSessionName, followRedirect, attackHttpParam, authAttackData.getData());
                performAttack(attack, httpMessages);
            }
        }

        return httpMessages;
    }
}