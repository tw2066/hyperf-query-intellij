<?php \Hyperf\DbConnection\Db::connection('goods')->table('goods as a')->selectRaw('jc_a.id, number');
\Hyperf\DbConnection\Db::connection('goods')->table('goods as a')->select(\Hyperf\DbConnection\Db::raw('jc_a.id,number'));
\Hyperf\DbConnection\Db::connection('goods')->table('goods as a')->selectRaw('jc_goods.name');