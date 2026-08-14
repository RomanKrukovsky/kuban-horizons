package dev.romankrukovsky.kubanhorizons.blockentity;

import dev.romankrukovsky.kubanhorizons.processing.GrapePressBlock;
import dev.romankrukovsky.kubanhorizons.processing.PressingRecipe;
import dev.romankrukovsky.kubanhorizons.registry.KHBlockEntities;
import dev.romankrukovsky.kubanhorizons.registry.KHRecipes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * Виноградный чан: давильня с накоплением сока.
 *
 * <p><b>Чем это не маслопресс.</b> Маслопресс — устройство партии: он держит
 * сырьё в слоте, копит прогресс до конца цикла и в один момент превращает
 * порцию семечек в бутылку. Пока цикл не завершён, у игрока нет ничего;
 * прервать цикл — значит потерять только прогресс. Чан устроен наоборот: у
 * него нет цикла вообще. Сырьё <b>не хранится</b> — брошенная в чан гроздь
 * немедленно раздавливается и перестаёт существовать как предмет, превращаясь
 * в число единиц сока в чане. Сок накапливается между сессиями работы и
 * <b>наливается отдельным действием</b>, когда его хватает на бутылку.</p>
 *
 * <p>Игровые следствия этой разницы, а не косметика:</p>
 * <ul>
 *   <li><b>Частичный прогресс материален.</b> Три грозди — это три единицы
 *       сока в чане, а не «недоделанная бутылка». Игрок может уйти, вернуться
 *       через неделю, добросить четвёртую и налить. У маслопресса
 *       незавершённая партия не имеет ценности до последнего тика.</li>
 *   <li><b>Порция не задана рецептом жёстко.</b> Одна гроздь даёт
 *       {@code juicePerItem} сока, бутылка стоит {@code juicePerBottle}; сырьё
 *       можно докладывать по одной единице. Маслопресс требует ровно
 *       {@code inputCount} семечек в слоте одновременно, иначе не начнёт.</li>
 *   <li><b>Работает от ног, а не от рук.</b> Давить можно прыжком в чан
 *       ({@link GrapePressBlock}), то есть устройство приводится в действие
 *       перемещением игрока, а не серией ПКМ по одной точке.</li>
 *   <li><b>Нет пассивного режима.</b> Маслопресс по конфигу работает сам
 *       (это станок с приводом). Чан не работает без человека никогда —
 *       давить некому. Поэтому здесь нет ни тикера, ни конфиг-флага.</li>
 *   <li><b>Уровень видно снаружи.</b> Заполнение чана — состояние блока
 *       {@link GrapePressBlock#LEVEL}, а не полоска в GUI. GUI нет вовсе.</li>
 * </ul>
 *
 * <p><b>Защита от дюпа.</b> Единственная точка создания предмета —
 * {@link #drawOff}, и она уменьшает {@code juice} <i>до</i> возврата бутылки:
 * состояния «сок списан, но бутылки нет» и «бутылка есть, но сок не списан»
 * не существует. Сырьё, наоборот, списывается в {@link #stomp} строго один
 * раз ({@code split(1)}) и не остаётся нигде: чан не хранит входной стек,
 * поэтому его нельзя вынуть обратно после давки. Сам сок — целое число, а не
 * предмет, поэтому его нельзя ни выронить, ни продублировать разбором блока:
 * при сломе чана недолитый сок пропадает, и это намеренно (иначе перестановка
 * блока была бы способом переносить жидкость без тары).</p>
 *
 * <p>Формат сохранения версионируется полем {@code SchemaVersion} (AD-006).</p>
 */
public class GrapePressBlockEntity extends BlockEntity {
    /** Версия схемы сохраняемых данных. */
    public static final int SCHEMA_VERSION = 1;

    /**
     * Ёмкость чана в единицах сока.
     *
     * <p>Выбрана так, чтобы чан вмещал несколько бутылок и имел смысл как
     * запас, но не превращался в бездонный бак: переполненный чан отказывается
     * принимать ягоды, и игрок обязан налить сок, а не давить бесконечно.</p>
     */
    public static final int CAPACITY = 16;

    /** Накопленный сок в единицах; предметом не является. */
    private int juice;

    /**
     * Рецепт, которым набран текущий сок.
     *
     * <p>Хранится, потому что чан не держит сырьё: без этой ссылки после
     * рестарта было бы неизвестно, <i>какой</i> сок налит, и чан наливал бы
     * первый подходящий. Смешивать сорта нельзя — см. {@link #stomp}.</p>
     */
    private @Nullable ResourceKey<Recipe<?>> juiceRecipe;

    private final RecipeManager.CachedCheck<SingleRecipeInput, PressingRecipe> quickCheck =
            RecipeManager.createCheck(KHRecipes.PRESSING_TYPE.get());

    public GrapePressBlockEntity(BlockPos pos, BlockState state) {
        super(KHBlockEntities.GRAPE_PRESS.get(), pos, state);
    }

    // --- Сериализация ---

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.getIntOr("SchemaVersion", 1);
        this.juice = Math.clamp(input.getIntOr("juice", 0), 0, CAPACITY);
        this.juiceRecipe = input.read("juice_recipe", Identifier.CODEC)
                .map(id -> ResourceKey.<Recipe<?>>create(Registries.RECIPE, id))
                .orElse(null);
        // Сок без рецепта прочитать нельзя (неизвестно, что наливать), поэтому
        // такая пара считается битой и обнуляется, а не наливается наугад.
        if (this.juiceRecipe == null) {
            this.juice = 0;
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("SchemaVersion", SCHEMA_VERSION);
        output.putInt("juice", this.juice);
        if (this.juiceRecipe != null) {
            output.store("juice_recipe", Identifier.CODEC, this.juiceRecipe.identifier());
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return this.saveWithoutMetadata(registries);
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    // --- Состояние ---

    /** Сколько единиц сока в чане. */
    public int juice() {
        return this.juice;
    }

    /** Сколько единиц сока ещё поместится. */
    public int freeSpace() {
        return CAPACITY - this.juice;
    }

    /** Готова ли хотя бы одна бутылка к наливу. */
    public boolean canDrawOff(ServerLevel level) {
        PressingRecipe recipe = this.currentRecipe(level);
        return recipe != null && this.juice >= recipe.juicePerBottle();
    }

    // --- Логика работы ---

    /**
     * Раздавить одну единицу сырья из стека {@code stack}.
     *
     * <p>Отказ (возврат {@code false}) означает одно из: нет рецепта давки для
     * этого предмета, чан полон, или в чане уже сок другого сорта. Последнее —
     * не техническое ограничение, а правило: смешивание превратило бы чан в
     * универсальный конвертер «что угодно → последний рецепт».</p>
     *
     * @return {@code true}, если единица сырья была раздавлена
     */
    public boolean stomp(ServerLevel level, ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        RecipeHolder<PressingRecipe> holder =
                this.quickCheck.getRecipeFor(new SingleRecipeInput(stack), level).orElse(null);
        if (holder == null) {
            return false;
        }
        if (this.juice > 0 && this.juiceRecipe != null && !this.juiceRecipe.equals(holder.id())) {
            return false;
        }
        int yield = holder.value().juicePerItem();
        if (yield > this.freeSpace()) {
            return false;
        }
        // Сырьё исчезает здесь и только здесь: чан не держит входной слот,
        // поэтому раздавленную гроздь физически невозможно достать обратно.
        stack.shrink(1);
        this.juice += yield;
        this.juiceRecipe = holder.id();
        this.markDirtyAndSync(level);
        return true;
    }

    /**
     * Налить одну бутылку из накопленного сока.
     *
     * <p>Требует пустую стеклянную бутылку как тару — сок сам себя не носит.
     * Тара списывается и сок уменьшается в одной операции до создания
     * результата.</p>
     *
     * @param bottles стек стеклянных бутылок в руке игрока
     * @return готовая бутылка сока либо {@link ItemStack#EMPTY}, если налить нечего
     */
    public ItemStack drawOff(ServerLevel level, ItemStack bottles) {
        if (bottles.isEmpty() || !bottles.is(Items.GLASS_BOTTLE)) {
            return ItemStack.EMPTY;
        }
        PressingRecipe recipe = this.currentRecipe(level);
        if (recipe == null || this.juice < recipe.juicePerBottle()) {
            return ItemStack.EMPTY;
        }
        // Порядок важен: сначала списываем и тару, и сок, затем создаём
        // результат. Промежуточного состояния с двойным учётом не возникает.
        this.juice -= recipe.juicePerBottle();
        bottles.shrink(1);
        if (this.juice == 0) {
            this.juiceRecipe = null;
        }
        this.markDirtyAndSync(level);
        return recipe.createBottle();
    }

    /**
     * Рецепт налитого сока.
     *
     * <p>Читается из менеджера рецептов по сохранённому ключу: если рецепт
     * убрали из датапака, сок становится ненаполняемым, но не превращается в
     * произвольный предмет.</p>
     */
    private @Nullable PressingRecipe currentRecipe(ServerLevel level) {
        if (this.juiceRecipe == null) {
            return null;
        }
        RecipeHolder<?> holder = level.getServer().getRecipeManager()
                .byKey(this.juiceRecipe).orElse(null);
        return holder != null && holder.value() instanceof PressingRecipe pressing ? pressing : null;
    }

    /**
     * Держит видимый уровень чана в согласии с содержимым.
     *
     * <p>Уровень — единственный интерфейс устройства: у чана нет GUI, поэтому
     * блок обязан сообщать о наполнении внешним видом.</p>
     */
    private void markDirtyAndSync(ServerLevel level) {
        this.setChanged();
        BlockState state = this.getBlockState();
        if (state.hasProperty(GrapePressBlock.LEVEL)) {
            int shown = GrapePressBlock.levelFor(this.juice, CAPACITY);
            if (state.getValue(GrapePressBlock.LEVEL) != shown) {
                level.setBlock(this.getBlockPos(),
                        state.setValue(GrapePressBlock.LEVEL, shown), 3);
                return;
            }
        }
        level.sendBlockUpdated(this.getBlockPos(), state, state, 3);
    }
}
