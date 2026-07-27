package mchorse.bbs_mod.entity;

import mchorse.bbs_mod.forms.FormUtils;
import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.forms.entities.MCEntity;
import mchorse.bbs_mod.forms.entities.StubEntity;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.items.GunProperties;
import mchorse.bbs_mod.network.ServerNetwork;
import mchorse.bbs_mod.utils.MathUtils;

import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.minecraft.class_1282;
import net.minecraft.class_1297;
import net.minecraft.class_1299;
import net.minecraft.class_1309;
import net.minecraft.class_1313;
import net.minecraft.class_1675;
import net.minecraft.class_1676;
import net.minecraft.class_1937;
import net.minecraft.class_2246;
import net.minecraft.class_2338;
import net.minecraft.class_2350;
import net.minecraft.class_238;
import net.minecraft.class_239;
import net.minecraft.class_2398;
import net.minecraft.class_243;
import net.minecraft.class_2487;
import net.minecraft.class_2680;
import net.minecraft.class_2945;
import net.minecraft.class_3222;
import net.minecraft.class_3483;
import net.minecraft.class_3532;
import net.minecraft.class_3959;
import net.minecraft.class_3965;
import net.minecraft.class_3966;
import net.minecraft.class_5134;

public class GunProjectileEntity extends class_1676 implements IEntityFormProvider
{
    private boolean despawn;
    private GunProperties properties = new GunProperties();
    private Form form;
    private IEntity stub = new StubEntity();
    private IEntity target = new MCEntity(this);

    private boolean stuck;
    private int lifeLeft;
    private int bounces;
    private class_2680 stuckBlockState;
    private boolean impacted;

    public GunProjectileEntity(class_1299<? extends class_1676> type, class_1937 world)
    {
        super(type, world);
    }

    private void vanish()
    {
        this.method_31472();
        this.executeCommand(this.properties.cmdVanish);
    }

    private void impact()
    {
        if (this.method_37908().field_9236)
        {
            return;
        }

        if (!this.impacted)
        {
            this.setForm(FormUtils.copy(this.properties.impactForm));

            for (class_3222 otherPlayer : PlayerLookup.tracking(this))
            {
                ServerNetwork.sendEntityForm(otherPlayer, this);
            }

            this.impacted = true;
        }

        this.executeCommand(this.properties.cmdImpact);
    }

    private void executeCommand(String command)
    {
        if (!command.isEmpty())
        {
            this.method_5682().method_3734().method_44252(this.method_5671(), command);
        }
    }

    @Override
    protected void method_5693(class_2945.class_9222 builder)
    {}

    public GunProperties getProperties()
    {
        return this.properties;
    }

    public void setProperties(GunProperties properties)
    {
        this.properties = properties;
        this.bounces = properties.bounces;
    }

    @Override
    public int getEntityId()
    {
        return this.method_5628();
    }

    @Override
    public Form getForm()
    {
        return this.form;
    }

    @Override
    public void setForm(Form form)
    {
        this.form = form;

        if (this.form != null)
        {
            this.form.playMain();
        }
    }

    public IEntity getEntity()
    {
        return this.properties.useTarget ? this.target : this.stub;
    }

    @Override
    protected int method_5691()
    {
        return 2;
    }

    @Override
    public boolean method_9200()
    {
        return false;
    }

    @Override
    public boolean method_5640(double distance)
    {
        return true;
    }

    @Override
    public void method_5773()
    {
        super.method_5773();

        this.getEntity().update();

        if (this.form != null)
        {
            this.form.update(this.getEntity());
        }

        if (!this.method_37908().field_9236)
        {
            this.lifeLeft += 1;

            int ticking = this.properties.ticking;

            if (ticking > 0 && this.lifeLeft % ticking == 0)
            {
                this.executeCommand(this.properties.cmdTicking);
            }

            if (this.lifeLeft >= this.properties.lifeSpan)
            {
                this.vanish();
            }
        }

        /* Movement code */
        class_243 v = this.method_18798();

        if (this.field_6004 == 0F && this.field_5982 == 0F)
        {
            this.method_36456(MathUtils.toDeg((float) class_3532.method_15349(v.field_1352, v.field_1350)));
            this.method_36457(MathUtils.toDeg((float) class_3532.method_15349(v.field_1351, v.method_37267())));

            this.field_5982 = this.method_36454();
            this.field_6004 = this.method_36455();
        }

        class_2338 blockPos = this.method_24515();
        class_2680 blockState = this.method_37908().method_8320(blockPos);
        class_243 pos;

        if (this.method_5721() || blockState.method_27852(class_2246.field_27879))
        {
            this.method_5646();
        }

        if (this.stuck && this.properties.collideBlocks)
        {
            if (this.stuckBlockState != blockState && this.shouldFall())
            {
                this.fall();
            }
        }
        else
        {
            class_243 oldPos = this.method_19538();

            pos = oldPos.method_1019(v);

            class_239 hitResult = class_1675.method_55054(this, this::method_26958, class_3959.class_3960.field_17558);

            if (hitResult.method_17783() != class_239.class_240.field_1333)
            {
                pos = hitResult.method_17784();
            }

            class_3966 entityHitResult = class_1675.method_18077(this.method_37908(), this, oldPos, pos, this.method_5829().method_18804(this.method_18798()).method_1014(1.0), this::method_26958);

            if (entityHitResult != null)
            {
                hitResult = entityHitResult;
            }

            boolean canCollide =
                (this.properties.collideBlocks && hitResult.method_17783() == class_239.class_240.field_1332) ||
                (this.properties.collideEntities && hitResult.method_17783() == class_239.class_240.field_1331);

            if (canCollide)
            {
                this.method_7488(hitResult);

                this.field_6007 = true;
            }

            v = this.method_18798();

            double x = this.method_23317() + v.field_1352;
            double y = this.method_23318() + v.field_1351;
            double z = this.method_23321() + v.field_1350;
            double d = v.method_37267();

            this.method_36456(MathUtils.toDeg((float) class_3532.method_15349(v.field_1352, v.field_1350)));
            this.method_36457(MathUtils.toDeg((float) class_3532.method_15349(v.field_1351, d)));
            this.method_36457(method_26960(this.field_6004, this.method_36455()));
            this.method_36456(method_26960(this.field_5982, this.method_36454()));

            float friction = this.properties.friction;
            float gravity = this.properties.gravity;

            if (this.method_5799())
            {
                for (int particles = 0; particles < 4; ++particles)
                {
                    float hitbox = 0.25F;

                    this.method_37908().method_8406(class_2398.field_11247, x - v.field_1352 * hitbox, y - v.field_1351 * hitbox, z - v.field_1350 * hitbox, v.field_1352, v.field_1351, v.field_1350);
                }

                friction = 0.6F;
            }

            this.method_18799(v.method_1021(friction).method_1023(0, gravity, 0));
            this.method_5814(x, y, z);
            this.method_5852();
        }
    }

    @Override
    public void method_5982()
    {
        super.method_5982();

        if (this.despawn)
        {
            this.method_31472();
        }
    }

    private boolean shouldFall()
    {
        return this.stuck && this.method_37908().method_18026((new class_238(this.method_19538(), this.method_19538())).method_1014(0.06));
    }

    private void fall()
    {
        class_243 v = this.method_18798();

        this.stuck = false;

        this.method_18799(v.method_18805(this.field_5974.method_43057() * 0.2F, this.field_5974.method_43057() * 0.2F, this.field_5974.method_43057() * 0.2F));
    }

    public void method_5784(class_1313 movementType, class_243 movement)
    {
        super.method_5784(movementType, movement);

        if (movementType != class_1313.field_6308 && this.shouldFall())
        {
            this.fall();
        }
    }

    @Override
    protected void method_7454(class_3966 entityHitResult)
    {
        super.method_7454(entityHitResult);

        if (this.method_37908().field_9236 || this.properties.damage <= 0F)
        {
            return;
        }

        class_1297 entity = entityHitResult.method_17782();
        float length = (float)this.method_18798().method_1033();
        int damage = class_3532.method_15386(class_3532.method_15363(length * this.properties.damage, 0, Integer.MAX_VALUE));

        class_1297 owner = this.method_24921();
        class_1282 source = this.method_48923().method_48831();

        int fireTicks = entity.method_20802();
        boolean deflectsArrows = entity.method_5864().method_20210(class_3483.field_48286);

        if (this.method_5809() && !deflectsArrows)
        {
            entity.method_5639(5);
        }

        if (entity.method_5643(source, (float) damage))
        {
            if (entity instanceof class_1309 livingEntity)
            {
                if (this.properties.knockback > 0)
                {
                    double resistanceFactor = Math.max(0D, 1D - livingEntity.method_45325(class_5134.field_23718));
                    class_243 punchVector = this.method_18798().method_1021(1D).method_1029().method_1021(this.properties.knockback * 0.6D * resistanceFactor);

                    if (punchVector.method_1027() > 0D)
                    {
                        livingEntity.method_5762(punchVector.field_1352, 0.1D, punchVector.field_1350);
                    }
                }

                this.onHit(livingEntity);
            }
        }
        else if (deflectsArrows)
        {
            this.deflect();
        }
        else
        {
            entity.method_20803(fireTicks);
            this.method_18799(this.method_18798().method_1021(-0.1D));
            this.method_36456(this.method_36454() + 180F);

            this.field_5982 += 180F;
        }
    }

    public void deflect()
    {
        float random = this.field_5974.method_43057() * 360F;

        this.method_18799(this.method_18798().method_1024(MathUtils.toRad(random)).method_1021(0.5D));
        this.method_36456(this.method_36454() + random);

        this.field_5982 += random;
    }

    @Override
    protected void method_24920(class_3965 blockHitResult)
    {
        super.method_24920(blockHitResult);

        class_243 velocity = blockHitResult.method_17784().method_1023(this.method_23317(), this.method_23318(), this.method_23321());

        if (this.bounces > 0)
        {
            this.bounces -= 1;

            velocity = this.method_18798();

            float damp = this.properties.bounceDamping;

            if (blockHitResult.method_17780().method_10166() == class_2350.class_2351.field_11048) velocity = velocity.method_18805(-damp, damp, damp);
            if (blockHitResult.method_17780().method_10166() == class_2350.class_2351.field_11052) velocity = velocity.method_18805(damp, -damp, damp);
            if (blockHitResult.method_17780().method_10166() == class_2350.class_2351.field_11051) velocity = velocity.method_18805(damp, damp, -damp);
        }
        else
        {
            this.stuckBlockState = this.method_37908().method_8320(blockHitResult.method_17777());
            this.stuck = true;

            if (this.properties.vanish)
            {
                this.vanish();
            }
        }

        this.method_18799(velocity);

        class_243 gravity = velocity.method_1029().method_1021(0.05D);

        this.method_23327(this.method_23317() - gravity.field_1352, this.method_23318() - gravity.field_1351, this.method_23321() - gravity.field_1350);
        this.impact();
    }

    protected void onHit(class_1309 target)
    {
        if (this.bounces <= 0 && this.properties.vanish)
        {
            this.vanish();
        }
        else
        {
            this.impact();
        }
    }

    @Override
    protected class_1297.class_5799 method_33570()
    {
        return class_5799.field_28630;
    }

    @Override
    public boolean method_5732()
    {
        return false;
    }

    @Override
    public void method_5837(class_3222 player)
    {
        super.method_5837(player);
        ServerNetwork.sendEntityForm(player, this);
        ServerNetwork.sendGunProperties(player, this);
    }

    @Override
    public void method_5749(class_2487 nbt)
    {
        super.method_5749(nbt);
        this.despawn = nbt.method_10577("despawn");
    }

    @Override
    public void method_5652(class_2487 nbt)
    {
        super.method_5652(nbt);
        nbt.method_10556("despawn", true);
    }
}